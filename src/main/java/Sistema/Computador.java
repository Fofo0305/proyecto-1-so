package Sistema;

import Modelo.Memoria;
import Modelo.Proceso;
import EDD.Cola;
import Modelo.EstadoProceso;
import Modelo.MotivoBloqueo;
import Modelo.ModoCPU;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.SemaforoSimulado;
import Modelo.ContextoBuffer;
import Modelo.EtapaBuffer;
import Planificacion.Politica;
import Planificacion.FCFS;
import java.util.concurrent.Semaphore;

/**
 * Computador reutilizable con identificación y RAM propias.
 * @author Adolfo
 */
public class Computador implements ComponenteReloj {
    private final Semaphore mutex = new Semaphore(1, true);
    private final int id;
    private final Memoria memoria;
    private final Cola<Proceso> nuevos = new Cola<>();
    private final Cola<Proceso> listos = new Cola<>();
    private final Cola<Proceso> bloqueados = new Cola<>();
    private final Cola<Proceso> terminados = new Cola<>();
    private record EsperaSemaforo(Proceso proceso, SemaforoSimulado semaforo) { }
    private final Cola<EsperaSemaforo> esperasSemaforos = new Cola<>();
    private final CPU cpu = new CPU();
    private final Politica politica = new FCFS();
    private final InterconexionBuffers interconexion;
    private int latenciaRed;
    private long ultimoCiclo;

    public Computador(int id, int capacidadRam) {
        this(id, capacidadRam, null);
    }

    public Computador(int id, int capacidadRam, InterconexionBuffers interconexion) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del computador debe ser positivo");
        }
        this.id = id;
        this.memoria = new Memoria(capacidadRam);
        this.interconexion = interconexion;
    }

    public void setLatenciaRed(int latencia) {
        if (latencia < 0) throw new IllegalArgumentException("La latencia no puede ser negativa");
        mutex.acquireUninterruptibly();
        try { latenciaRed = latencia; } finally { mutex.release(); }
    }

    /**
     * Recibe un proceso asignado a este computador y lo deja en nuevos sin reservar RAM.
     * @param proceso proceso en estado NUEVO que se incorpora
     */
    public void recibirProceso(Proceso proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso es obligatorio");
        }
        mutex.acquireUninterruptibly();
        try {
            if (proceso.getComputador() != id || proceso.getEstado() != EstadoProceso.NUEVO) {
                throw new IllegalArgumentException("El proceso debe ser nuevo y pertenecer a este computador");
            }
            if (contiene(nuevos, proceso.getId()) || contiene(listos, proceso.getId())
                    || contiene(bloqueados, proceso.getId()) || contiene(terminados, proceso.getId())
                    || cpu.getActual() == proceso) {
                throw new IllegalArgumentException("El proceso ya pertenece al computador");
            }
            nuevos.agregar(proceso);
        } finally {
            mutex.release();
        }
    }

    /**
     * Revisa una vez los nuevos y admite los que caben en RAM, en orden de llegada.
     * Los que no caben permanecen en nuevos conservando su orden relativo.
     * @return cantidad de procesos que pasaron a listos
     */
    public int admitirProcesos() {
        mutex.acquireUninterruptibly();
        try {
            return admitirPendientes();
        } finally {
            mutex.release();
        }
    }

    private int admitirPendientes() {
        int admitidos = 0;
        int pendientes = nuevos.getTamano();
        for (int i = 0; i < pendientes; i++) {
            Proceso proceso = nuevos.primero();
            if (memoria.reservar("proceso:" + proceso.getId(), proceso.getMemoria())) {
                proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
                nuevos.quitar();
                listos.agregar(proceso);
                admitidos++;
            } else {
                nuevos.agregar(nuevos.quitar());
            }
        }
        return admitidos;
    }

    /**
     * Termina un proceso registrado, libera su reserva y vuelve a revisar los nuevos.
     * El núcleo solicita esta operación al finalizar, vencer o cancelar un proceso.
     * @param proceso proceso que se retira de las colas activas
     * @return false si ya estaba en terminados; true si se terminó ahora
     */
    public boolean terminarProceso(Proceso proceso) {
        if (proceso == null || proceso.getComputador() != id) {
            throw new IllegalArgumentException("El proceso debe pertenecer a este computador");
        }
        mutex.acquireUninterruptibly();
        try {
            boolean resultado = terminarRegistrado(proceso);
            admitirPendientes();
            return resultado;
        } finally {
            mutex.release();
        }
    }

    private boolean terminarRegistrado(Proceso proceso) {
        if (contiene(terminados, proceso.getId())) return false;
        if (!contiene(nuevos, proceso.getId()) && !contiene(listos, proceso.getId())
                && !contiene(bloqueados, proceso.getId()) && cpu.getActual() != proceso)
            throw new IllegalArgumentException("El proceso no esta registrado en el computador");
        EsperaSemaforo espera = retirarEsperaSemaforo(proceso);
        if (espera != null) espera.semaforo().cancelarEspera(proceso);
        proceso.cambiarEstado(EstadoProceso.TERMINADO, MotivoBloqueo.NINGUNO);
        retirar(nuevos, proceso.getId());
        retirar(listos, proceso.getId());
        retirar(bloqueados, proceso.getId());
        if (cpu.getActual() == proceso) cpu.retirar();
        if (proceso.getContextoBuffer() != null)
            for (SemaforoSimulado semaforo : proceso.getContextoBuffer().abandonar()) senalizarBuffer(semaforo);
        memoria.liberar("proceso:" + proceso.getId());
        terminados.agregar(proceso);
        return true;
    }

    /**
     * Consume un permiso o bloquea al proceso actual y libera la CPU.
     * El semaforo nunca llama al nucleo mientras conserva su mutex: las senales
     * entregan primero el receptor y despues solicitan el despertar.
     * Esta transicion no ejecuta una instruccion; el motor contabiliza su ciclo.
     */
    public boolean esperarSemaforo(Proceso proceso, SemaforoSimulado semaforo) {
        if (proceso == null || semaforo == null)
            throw new IllegalArgumentException("Proceso y semaforo obligatorios");
        mutex.acquireUninterruptibly();
        try {
            if (cpu.getActual() != proceso || proceso.getEstado() != EstadoProceso.EJECUCION
                    || proceso.getDeadline() == 0 || proceso.getRestante() == 0)
                throw new IllegalStateException("Solo puede esperar el proceso vigente en CPU");
            return esperarRegistrado(proceso, semaforo);
        } finally { mutex.release(); }
    }

    private boolean esperarRegistrado(Proceso proceso, SemaforoSimulado semaforo) {
        if (semaforo.esperar(proceso)) return true;
        esperasSemaforos.agregar(new EsperaSemaforo(proceso, semaforo));
        proceso.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.SEMAFORO);
        cpu.retirar();
        bloqueados.agregar(proceso);
        return false;
    }

    /**
     * Acepta un permiso transferido solo para la espera registrada en ese semaforo.
     * Si el proceso termino mientras llegaba la senal, devuelve false: quien senala
     * debe transferir de nuevo el permiso, sin perderlo.
     */
    public boolean despertarPorSemaforo(Proceso proceso, SemaforoSimulado semaforo) {
        if (proceso == null || semaforo == null)
            throw new IllegalArgumentException("Proceso y semaforo obligatorios");
        mutex.acquireUninterruptibly();
        try {
            return despertarRegistrado(proceso, semaforo);
        } finally { mutex.release(); }
    }

    private boolean despertarRegistrado(Proceso proceso, SemaforoSimulado semaforo) {
            if (proceso.getEstado() != EstadoProceso.BLOQUEADO
                    || proceso.getMotivoBloqueo() != MotivoBloqueo.SEMAFORO
                    || !contiene(bloqueados, proceso.getId())) return false;
            boolean coincide = false;
            for (Object elemento : esperasSemaforos.instantanea()) {
                EsperaSemaforo espera = (EsperaSemaforo) elemento;
                if (espera.proceso() == proceso && espera.semaforo() == semaforo) coincide = true;
            }
            if (!coincide) return false;
            if (proceso.getContextoBuffer() != null) proceso.getContextoBuffer().conceder(semaforo);
            retirarEsperaSemaforo(proceso);
            retirar(bloqueados, proceso.getId());
            proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
            listos.agregar(proceso);
            return true;
    }

    private EsperaSemaforo retirarEsperaSemaforo(Proceso proceso) {
        EsperaSemaforo retirada = null;
        int cantidad = esperasSemaforos.getTamano();
        for (int i = 0; i < cantidad; i++) {
            EsperaSemaforo espera = esperasSemaforos.quitar();
            if (espera.proceso() == proceso) retirada = espera;
            else esperasSemaforos.agregar(espera);
        }
        return retirada;
    }

    /** Ejecuta una instrucción FCFS y descuenta una vez todos los plazos del ciclo. */
    public void avanzarCiclo(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (ciclo != ultimoCiclo + 1) throw new IllegalArgumentException("El ciclo debe ser consecutivo");
            for (Object elemento : nuevos.instantanea()) validarTipoEjecucion((Proceso) elemento);
            for (Object elemento : listos.instantanea()) validarTipoEjecucion((Proceso) elemento);
            admitirPendientes();
            Object[][] colasCiclo = {nuevos.instantanea(), listos.instantanea(), bloqueados.instantanea()};
            Proceso anterior = cpu.getActual();
            if (anterior != null) prepararAccesoRemoto(anterior, ciclo);
            while (cpu.getActual() == null) {
                Proceso seleccionado = politica.seleccionar(listos);
                if (seleccionado == null) break;
                cpu.asignar(seleccionado);
                if (!prepararAccesoRemoto(seleccionado, ciclo)) break;
            }
            Proceso actual = cpu.getActual();
            if (actual != null && actual.getContextoBuffer() != null) {
                ContextoBuffer contexto = actual.getContextoBuffer();
                if (contexto.getEtapa() == EtapaBuffer.INTERVALO) {
                    cpu.avanzarCiclo(ciclo);
                    contexto.avanzarIntervalo();
                } else {
                    cpu.avanzarCicloSistema(ciclo, () -> ejecutarOperacionBuffer(actual));
                }
            } else cpu.avanzarCiclo(ciclo);
            for (Object[] cola : colasCiclo)
                for (Object elemento : cola) ((Proceso) elemento).descontarDeadline();
            if (anterior != null) anterior.descontarDeadline();
            Proceso ejecutado = cpu.getActual();
            if (ejecutado != null && ejecutado.getRestante() == 0) terminarRegistrado(ejecutado);
            for (Object[] cola : colasCiclo) {
                for (Object elemento : cola) {
                    Proceso proceso = (Proceso) elemento;
                    if (proceso.getEstado() != EstadoProceso.TERMINADO && proceso.getDeadline() == 0)
                        terminarRegistrado(proceso);
                }
            }
            if (anterior != null && anterior.getEstado() != EstadoProceso.TERMINADO && anterior.getDeadline() == 0)
                terminarRegistrado(anterior);
            finalizarEsperasRed(ciclo);
            admitirPendientes();
            ultimoCiclo = ciclo;
        } finally { mutex.release(); }
    }

    private void validarTipoEjecucion(Proceso proceso) {
        if (proceso.getTipo() == TipoProceso.IO_BOUND)
            throw new UnsupportedOperationException("Falta el modulo de IO_BOUND");
    }

    private void ejecutarOperacionBuffer(Proceso proceso) {
        ContextoBuffer contexto = proceso.getContextoBuffer();
        // Incluso un wait que bloquea consume su instruccion exactamente una vez.
        proceso.ejecutarInstruccion();
        switch (contexto.getEtapa()) {
            case ESPERAR_RECURSO, ESPERAR_MUTEX -> {
                SemaforoSimulado semaforo = contexto.getEtapa() == EtapaBuffer.ESPERAR_RECURSO
                        ? contexto.getRecurso() : contexto.getBuffer().getMutexSimulado();
                contexto.iniciarEspera();
                if (esperarRegistrado(proceso, semaforo)) contexto.conceder(semaforo);
            }
            case MODIFICAR -> contexto.modificar(proceso.getId());
            case SENALAR_MUTEX -> {
                contexto.liberarMutex();
                senalizarBuffer(contexto.getBuffer().getMutexSimulado());
            }
            case SENALAR_RECURSO -> {
                SemaforoSimulado liberado = contexto.getRecursoLiberado();
                contexto.completar();
                senalizarBuffer(liberado);
            }
            default -> throw new IllegalStateException("Etapa no ejecutable en modo sistema");
        }
    }

    /** Envia despertares remotos a la barrera; no adquiere mutex de otros computadores. */
    private void senalizarBuffer(SemaforoSimulado semaforo) {
        Proceso receptor;
        while ((receptor = semaforo.senalizar()) != null) {
            if (receptor.getComputador() != id) {
                if (interconexion == null) throw new IllegalStateException("Falta interconexion del cluster");
                interconexion.enviar(receptor, semaforo);
                return;
            }
            if (despertarRegistrado(receptor, semaforo)) return;
        }
    }

    private boolean prepararAccesoRemoto(Proceso proceso, long ciclo) {
        ContextoBuffer contexto = proceso.getContextoBuffer();
        if (contexto == null || proceso.getBuffer().getAnfitrion() == id
                || contexto.getEtapa() != EtapaBuffer.ESPERAR_RECURSO || contexto.isRedPreparada()) return false;
        contexto.prepararRed(ciclo, latenciaRed);
        if (latenciaRed == 0) return false;
        proceso.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.RED);
        cpu.retirar();
        bloqueados.agregar(proceso);
        return true;
    }

    /** La espera incluye el ciclo de inicio y permite ejecutar desde el siguiente al vencimiento. */
    private void finalizarEsperasRed(long ciclo) {
        for (Object elemento : bloqueados.instantanea()) {
            Proceso proceso = (Proceso) elemento;
            if (proceso.getMotivoBloqueo() == MotivoBloqueo.RED
                    && proceso.getContextoBuffer().getRedHasta() <= ciclo) {
                retirar(bloqueados, proceso.getId());
                proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
                listos.agregar(proceso);
            }
        }
    }

    public Proceso getProcesoEnCPU() {
        mutex.acquireUninterruptibly();
        try { return cpu.getActual(); } finally { mutex.release(); }
    }

    /** Punto de apropiacion del planificador: conserva PC, etapa y permisos adquiridos. */
    public boolean cederCPU() {
        mutex.acquireUninterruptibly();
        try {
            Proceso actual = cpu.getActual();
            if (actual == null) return false;
            actual.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
            cpu.retirar();
            listos.agregar(actual);
            return true;
        } finally { mutex.release(); }
    }

    public ModoCPU getModoCPU() {
        mutex.acquireUninterruptibly();
        try { return cpu.getModo(); } finally { mutex.release(); }
    }

    public long getUltimoCiclo() {
        mutex.acquireUninterruptibly();
        try { return ultimoCiclo; } finally { mutex.release(); }
    }

    private void retirar(Cola<Proceso> cola, long idProceso) {
        int cantidad = cola.getTamano();
        for (int i = 0; i < cantidad; i++) {
            Proceso proceso = cola.quitar();
            if (proceso.getId() != idProceso) cola.agregar(proceso);
        }
    }

    private boolean contiene(Cola<Proceso> cola, long idProceso) {
        for (Object elemento : cola.instantanea()) {
            if (((Proceso) elemento).getId() == idProceso) return true;
        }
        return false;
    }

    /** Captura CPU, colas y RAM bajo el mismo bloqueo del núcleo. */
    public VistaComputador consultarVista() {
        mutex.acquireUninterruptibly();
        try {
            int[] ram = memoria.instantanea();
            Proceso actual = cpu.getActual();
            return new VistaComputador(id, ultimoCiclo, cpu.getModo(), ram[0], ram[1],
                    actual == null ? "Sin proceso" : describirProceso(actual),
                    describirCola(nuevos), describirCola(listos), describirCola(bloqueados), describirCola(terminados),
                    actual == null ? null : vistaProceso(actual), vistasCola(nuevos), vistasCola(listos),
                    vistasCola(bloqueados), vistasCola(terminados));
        } finally { mutex.release(); }
    }

    private Modelo.VistaProceso vistaProceso(Proceso proceso) {
        return new Modelo.VistaProceso(proceso.getId(), proceso.getNombre(), proceso.getMemoria(),
                proceso.getPrioridad(), proceso.getDeadline(), proceso.getRestante(), proceso.getPc(),
                proceso.getMar(), proceso.getEstado(), proceso.getTipo(), proceso.getMotivoBloqueo(), describirOperacion(proceso));
    }

    private String describirOperacion(Proceso proceso) {
        ContextoBuffer contexto = proceso.getContextoBuffer();
        if (contexto == null) return "";
        String texto = "Buffer " + proceso.getBuffer().getNombre() + " | " + contexto.getEtapa()
                + " | elementos " + contexto.getCompletados() + "/" + proceso.getParametrosBuffer().elementos();
        for (Object elemento : esperasSemaforos.instantanea()) {
            EsperaSemaforo espera = (EsperaSemaforo) elemento;
            if (espera.proceso() == proceso) texto += " | espera " + espera.semaforo().getNombre();
        }
        return texto;
    }

    public Modelo.VistaBuffer consultarBuffer(Modelo.Buffer<?> buffer) {
        if (buffer == null || buffer.getAnfitrion() != id)
            throw new IllegalArgumentException("El buffer debe pertenecer a este computador");
        mutex.acquireUninterruptibly();
        try {
            StringBuilder contenido = new StringBuilder();
            for (Object elemento : buffer.instantanea()) {
                if (!contenido.isEmpty()) contenido.append(", ");
                contenido.append(elemento);
            }
            return new Modelo.VistaBuffer(buffer.getNombre(), id, buffer.getCapacidad(), buffer.getCantidad(),
                    contenido.toString(), buffer.getMutexSimulado().getValor(), buffer.getVacios().getValor(),
                    buffer.getLlenos().getValor(), describirEspera(buffer.getMutexSimulado()),
                    describirEspera(buffer.getVacios()), describirEspera(buffer.getLlenos()));
        } finally { mutex.release(); }
    }

    private String describirEspera(SemaforoSimulado semaforo) {
        StringBuilder texto = new StringBuilder();
        for (Proceso proceso : semaforo.getEsperando()) {
            if (!texto.isEmpty()) texto.append(", ");
            texto.append("P").append(proceso.getId()).append(" ").append(proceso.getNombre());
        }
        return texto.isEmpty() ? "sin esperas" : texto.toString();
    }

    private Modelo.VistaProceso[] vistasCola(Cola<Proceso> cola) {
        Object[] elementos = cola.instantanea();
        Modelo.VistaProceso[] vistas = new Modelo.VistaProceso[elementos.length];
        for (int i = 0; i < elementos.length; i++) vistas[i] = vistaProceso((Proceso) elementos[i]);
        return vistas;
    }

    private String describirCola(Cola<Proceso> cola) {
        StringBuilder texto = new StringBuilder();
        for (Object elemento : cola.instantanea()) {
            if (!texto.isEmpty()) texto.append('\n');
            texto.append(describirProceso((Proceso) elemento));
        }
        return texto.isEmpty() ? "Vacia" : texto.toString();
    }

    private String describirProceso(Proceso proceso) {
        return "P" + proceso.getId() + " " + proceso.getNombre() + " | computador=" + proceso.getComputador()
                + " | " + proceso.getTipo() + " | " + proceso.getEstado() + " | prioridad=" + proceso.getPrioridad()
                + " | RAM=" + proceso.getMemoria() + " | deadline=" + proceso.getDeadline()
                + " | restante=" + proceso.getRestante() + " | PC=" + proceso.getPc() + " | MAR=" + proceso.getMar()
                + " | bloqueo=" + proceso.getMotivoBloqueo() + " | " + describirOperacion(proceso);
    }

    public int getId() { return id; }
    public Memoria getMemoria() { return memoria; }

    /** Consulta los procesos nuevos sin exponer la cola interna. */
    public Proceso[] getNuevos() { return copiarProcesos(nuevos); }

    /** Consulta los procesos listos sin exponer la cola interna. */
    public Proceso[] getListos() { return copiarProcesos(listos); }

    /** Consulta los procesos bloqueados sin exponer la cola interna. */
    public Proceso[] getBloqueados() { return copiarProcesos(bloqueados); }

    /** Consulta los procesos terminados sin exponer la cola interna. */
    public Proceso[] getTerminados() { return copiarProcesos(terminados); }

    private Proceso[] copiarProcesos(Cola<Proceso> cola) {
        mutex.acquireUninterruptibly();
        try {
            Object[] elementos = cola.instantanea();
            Proceso[] copia = new Proceso[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                copia[i] = (Proceso) elementos[i];
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
}
