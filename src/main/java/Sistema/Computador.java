package Sistema;

import EDD.Cola;
import Modelo.AreaDisco;
import Modelo.EstadoProceso;
import Modelo.EventoSistema;
import Modelo.Memoria;
import Modelo.ModoCPU;
import Modelo.MotivoBloqueo;
import Modelo.OperacionES;
import Modelo.ParametrosArchivo;
import Modelo.ParametrosES;
import Modelo.PlanOperacionArchivo;
import Modelo.PoliticaPlanificacion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.VistaMetricas;
import Modelo.VistaSolicitudDisco;
import Planificacion.FCFS;
import Planificacion.Politica;
import java.util.concurrent.Semaphore;

/** Núcleo del único computador de ÁvilaOS. @author Adolfo */
public class Computador implements ComponenteReloj {
    private final Semaphore mutex = new Semaphore(1, true);
    private final int id;
    private final Memoria memoria;
    private final Cola<Proceso> nuevos = new Cola<>();
    private final Cola<Proceso> listos = new Cola<>();
    private final Cola<Proceso> bloqueados = new Cola<>();
    private final Cola<Proceso> listosSuspendidos = new Cola<>();
    private final Cola<Proceso> bloqueadosSuspendidos = new Cola<>();
    private final Cola<Proceso> terminados = new Cola<>();
    private final Cola<Proceso> todos = new Cola<>();
    private final CPU cpu = new CPU();
    private final RegistroSistema log = new RegistroSistema();
    private Politica politica = new FCFS();
    private PoliticaPlanificacion tipoPolitica = PoliticaPlanificacion.FCFS;
    private int quantum = 4;
    private int usadosQuantum;
    private long ocupados;
    private final long[] historiaCiclos = new long[600];
    private final double[] historiaUso = new double[600];
    private int historiaInicio;
    private int historiaTamano;
    private long ultimoCiclo;
    private long cicloEnCurso = -1;
    private Disco disco;
    private SistemaArchivos sistemaArchivos;

    public Computador(int id, int capacidadRam) {
        if (id != 1) throw new IllegalArgumentException("AvilaOS utiliza un unico computador con ID 1");
        this.id = id;
        memoria = new Memoria(capacidadRam);
    }

    /**
     * Conecta el disco y el sistema de archivos. Sin ellos el computador solo simula CPU y
     * E/S generica, como en el Proyecto 1.
     */
    public void conectarAlmacenamiento(Disco nuevoDisco, SistemaArchivos nuevoSistema) {
        if (nuevoDisco == null || nuevoSistema == null)
            throw new IllegalArgumentException("El disco y el sistema de archivos son obligatorios");
        mutex.acquireUninterruptibly();
        try {
            if (ultimoCiclo > 0) throw new IllegalStateException("El almacenamiento se conecta antes de iniciar");
            disco = nuevoDisco;
            sistemaArchivos = nuevoSistema;
        } finally { mutex.release(); }
    }

    public void recibirProceso(Proceso proceso) {
        if (proceso == null || proceso.getComputador() != id || proceso.getEstado() != EstadoProceso.NUEVO
                || !proceso.tieneConfiguracionValida())
            throw new IllegalArgumentException("El proceso debe estar configurado, ser nuevo y pertenecer al computador");
        mutex.acquireUninterruptibly();
        try {
            if (registrado(proceso) || contiene(terminados, proceso.getId()))
                throw new IllegalArgumentException("El proceso ya pertenece al computador");
            proceso.registrarLlegada(ultimoCiclo);
            todos.agregar(proceso);
            nuevos.agregar(proceso);
            log.agregar(ultimoCiclo, id, proceso.getId(), "CREACION", proceso.getNombre());
        } finally { mutex.release(); }
    }

    public int admitirProcesos() {
        mutex.acquireUninterruptibly();
        try { return admitirPendientes(); } finally { mutex.release(); }
    }

    /**
     * Planificador a largo y a mediano plazo. Dentro de un ciclo, y con disco conectado,
     * primero devuelve a la RAM a los listos/suspendidos que caben y despues admite nuevos;
     * si un nuevo no cabe, suspende procesos bloqueados para hacerle espacio.
     */
    private int admitirPendientes() {
        boolean medianoPlazo = cicloEnCurso >= 0 && disco != null;
        if (medianoPlazo) reactivarSuspendidos(cicloEnCurso);
        int admitidos = 0;
        int pendientes = nuevos.getTamano();
        for (int i = 0; i < pendientes; i++) {
            Proceso proceso = nuevos.primero();
            boolean cabe = memoria.reservar("proceso:" + proceso.getId(), proceso.getMemoria());
            if (!cabe && medianoPlazo && suspenderPara(proceso))
                cabe = memoria.reservar("proceso:" + proceso.getId(), proceso.getMemoria());
            if (cabe) {
                nuevos.quitar();
                proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
                listos.agregar(proceso);
                politica.ordenar(listos);
                log.agregar(cicloEnCurso >= 0 ? cicloEnCurso : ultimoCiclo, id, proceso.getId(), "ADMISION", "RAM reservada");
                admitidos++;
            } else nuevos.agregar(nuevos.quitar());
        }
        return admitidos;
    }

    /**
     * Elige a quien suspender para que entre {@code nuevo}. Criterio: el que lleva mas tiempo
     * bloqueado (frente de la cola de bloqueados), porque es el que menos ha avanzado desde que
     * dejo la CPU y su espera no depende de estar en RAM. Se salta a quien no cabe en el swap
     * y solo se suspende si con los elegidos el nuevo cabe; si no, nadie sale de la RAM.
     * @return true si suspendio a alguien
     */
    private boolean suspenderPara(Proceso nuevo) {
        if (nuevo.getMemoria() > memoria.getCapacidad()) return false;
        int faltan = nuevo.getMemoria() - memoria.getLibre();
        int swapLibre = disco.contarLibres(AreaDisco.SWAP);
        Object[] candidatos = bloqueados.instantanea();
        boolean[] elegidos = new boolean[candidatos.length];
        int liberables = 0;
        for (int i = 0; i < candidatos.length && liberables < faltan; i++) {
            int ram = ((Proceso) candidatos[i]).getMemoria();
            if (ram > swapLibre) continue;
            elegidos[i] = true;
            liberables += ram;
            swapLibre -= ram;
        }
        if (liberables < faltan) return false;
        for (int i = 0; i < candidatos.length; i++)
            if (elegidos[i]) suspender((Proceso) candidatos[i], nuevo);
        return true;
    }

    /** Bloqueado → Bloqueado/Suspendido: copia su RAM al swap con una solicitud SUSPENDER. */
    private void suspender(Proceso proceso, Proceso nuevo) {
        retirar(bloqueados, proceso.getId());
        proceso.cambiarEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO, proceso.getMotivoBloqueo());
        memoria.liberar("proceso:" + proceso.getId());
        int primero = disco.asignarSwap("P" + proceso.getId(), proceso.getMemoria());
        long solicitud = disco.encolarSolicitud(proceso.getId(), OperacionES.SUSPENDER, primero,
                proceso.getMemoria(), cicloEnCurso);
        proceso.registrarSwap(primero, solicitud);
        bloqueadosSuspendidos.agregar(proceso);
        log.agregar(cicloEnCurso, id, proceso.getId(), "SUSPENSION", "Se suspende al proceso P" + proceso.getId()
                + " para admitir a P" + nuevo.getId() + ": S" + solicitud + " escribe " + proceso.getMemoria()
                + " bloques de swap desde " + primero);
    }

    /**
     * Listo/Suspendido → Listo: reserva su RAM y pide al disco leer su imagen (REACTIVAR).
     * Solo se reactiva a quien ya termino de escribirse en swap y cabe en la RAM libre.
     */
    private void reactivarSuspendidos(long ciclo) {
        for (Object elemento : listosSuspendidos.instantanea()) {
            Proceso proceso = (Proceso) elemento;
            if (proceso.getSolicitudSwap() != 0
                    || !memoria.reservar("proceso:" + proceso.getId(), proceso.getMemoria())) continue;
            long solicitud = disco.encolarSolicitud(proceso.getId(), OperacionES.REACTIVAR,
                    proceso.getBloqueSwap(), proceso.getMemoria(), ciclo);
            proceso.registrarReactivacion(solicitud);
            log.agregar(ciclo, id, proceso.getId(), "REACTIVACION", "Hay RAM para P" + proceso.getId()
                    + ": S" + solicitud + " lee " + proceso.getMemoria() + " bloques de swap desde "
                    + proceso.getBloqueSwap());
        }
    }

    /** Resultado en el disco de una solicitud SUSPENDER o REACTIVAR. */
    private void atenderSwap(VistaSolicitudDisco completada, long ciclo) {
        Proceso proceso = null;
        for (Cola<Proceso> cola : colasSuspendidas())
            for (Object elemento : cola.instantanea())
                if (((Proceso) elemento).getId() == completada.procesoId()
                        && ((Proceso) elemento).getSolicitudSwap() == completada.id())
                    proceso = (Proceso) elemento;
        if (proceso == null) return;
        if (completada.operacion() == OperacionES.SUSPENDER) {
            proceso.terminarSolicitudSwap();
            log.agregar(ciclo, id, proceso.getId(), "FIN_SUSPENSION", "S" + completada.id()
                    + " imagen escrita en swap");
            return;
        }
        liberarSwap(proceso);
        retirar(listosSuspendidos, proceso.getId());
        proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        listos.agregar(proceso);
        log.agregar(ciclo, id, proceso.getId(), "FIN_REACTIVACION", "S" + completada.id()
                + " vuelve a la RAM y a listos");
    }

    @SuppressWarnings("unchecked")
    private Cola<Proceso>[] colasSuspendidas() { return new Cola[] {listosSuspendidos, bloqueadosSuspendidos}; }

    private void liberarSwap(Proceso proceso) {
        if (proceso.getBloqueSwap() < 0) return;
        disco.liberarEncadenado(proceso.getBloqueSwap());
        proceso.olvidarSwap();
    }

    /** Su evento ocurrio: Bloqueado → Listo, o Bloqueado/Suspendido → Listo/Suspendido. */
    private void despertar(Proceso proceso, long ciclo, String tipo, String detalle) {
        if (proceso.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            retirar(bloqueadosSuspendidos, proceso.getId());
            proceso.cambiarEstado(EstadoProceso.LISTO_SUSPENDIDO, MotivoBloqueo.NINGUNO);
            listosSuspendidos.agregar(proceso);
            if (tipo != null) log.agregar(ciclo, id, proceso.getId(), tipo, detalle + ": pasa a listo/suspendido");
            return;
        }
        retirar(bloqueados, proceso.getId());
        proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        listos.agregar(proceso);
        if (tipo != null) log.agregar(ciclo, id, proceso.getId(), tipo, detalle + ": vuelve a listos");
    }

    private boolean registrado(Proceso proceso) {
        return contiene(nuevos, proceso.getId()) || contiene(listos, proceso.getId())
                || contiene(bloqueados, proceso.getId()) || contiene(listosSuspendidos, proceso.getId())
                || contiene(bloqueadosSuspendidos, proceso.getId()) || cpu.getActual() == proceso;
    }

    public boolean terminarProceso(Proceso proceso) {
        if (proceso == null || proceso.getComputador() != id)
            throw new IllegalArgumentException("El proceso debe pertenecer al computador");
        mutex.acquireUninterruptibly();
        try {
            boolean terminado = terminarRegistrado(proceso);
            admitirPendientes();
            return terminado;
        } finally { mutex.release(); }
    }

    private boolean terminarRegistrado(Proceso proceso) {
        if (contiene(terminados, proceso.getId())) return false;
        if (!registrado(proceso))
            throw new IllegalArgumentException("El proceso no esta registrado en el computador");
        proceso.cerrarResultado();
        log.agregar(cicloEnCurso >= 0 ? cicloEnCurso : ultimoCiclo, id, proceso.getId(), "TERMINACION", proceso.getResultado());
        proceso.cambiarEstado(EstadoProceso.TERMINADO, MotivoBloqueo.NINGUNO);
        retirar(nuevos, proceso.getId());
        retirar(listos, proceso.getId());
        retirar(bloqueados, proceso.getId());
        retirar(listosSuspendidos, proceso.getId());
        retirar(bloqueadosSuspendidos, proceso.getId());
        if (cpu.getActual() == proceso) { cpu.retirar(); usadosQuantum = 0; }
        memoria.liberar("proceso:" + proceso.getId());
        if (disco != null) liberarSwap(proceso);
        terminados.agregar(proceso);
        return true;
    }

    @Override
    public void avanzarCiclo(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (ciclo != ultimoCiclo + 1) throw new IllegalArgumentException("El ciclo debe ser consecutivo");
            cicloEnCurso = ciclo;
            atenderCompletadas(ciclo);
            admitirPendientes();
            Object[][] colasCiclo = {nuevos.instantanea(), listos.instantanea(), bloqueados.instantanea(),
                    listosSuspendidos.instantanea(), bloqueadosSuspendidos.instantanea()};
            Proceso anterior = cpu.getActual();
            politica.ordenar(listos);
            if (anterior != null && politica.debeApropiar(anterior, listos, usadosQuantum, quantum)) {
                anterior.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
                cpu.retirar();
                listos.agregar(anterior);
                politica.ordenar(listos);
                usadosQuantum = 0;
                log.agregar(ciclo, id, anterior.getId(), "APROPIACION", tipoPolitica.name());
            }
            if (cpu.getActual() == null) {
                Proceso seleccionado = politica.seleccionar(listos);
                if (seleccionado != null) {
                    cpu.asignar(seleccionado);
                    usadosQuantum = 0;
                    log.agregar(ciclo, id, seleccionado.getId(), "DESPACHO", tipoPolitica.name());
                }
            }
            Proceso actual = cpu.getActual();
            if (actual != null) {
                actual.registrarRespuesta(ciclo);
                ocupados++;
                usadosQuantum++;
            }
            if (disco != null && actual != null && actual.necesitaSolicitarArchivo()) {
                boolean[] encolada = new boolean[1];
                cpu.avanzarCicloSistema(ciclo, () -> encolada[0] = solicitarArchivo(actual, ciclo));
                if (encolada[0]) {
                    actual.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.DISCO);
                    cpu.retirar();
                    usadosQuantum = 0;
                    bloqueados.agregar(actual);
                }
            } else cpu.avanzarCiclo(ciclo);
            for (Object[] cola : colasCiclo)
                for (Object elemento : cola) ((Proceso) elemento).descontarDeadline();
            if (anterior != null) anterior.descontarDeadline();
            Proceso ejecutado = cpu.getActual();
            if (ejecutado != null && ejecutado.getRestante() == 0) terminarRegistrado(ejecutado);
            for (Object[] cola : colasCiclo)
                for (Object elemento : cola) {
                    Proceso proceso = (Proceso) elemento;
                    if (proceso.getEstado() != EstadoProceso.TERMINADO && proceso.getDeadline() == 0)
                        terminarRegistrado(proceso);
                }
            if (anterior != null && anterior.getEstado() != EstadoProceso.TERMINADO && anterior.getDeadline() == 0)
                terminarRegistrado(anterior);
            finalizarES(ciclo);
            Proceso io = cpu.getActual();
            if (io != null && io.agotarRafaga(ciclo)) {
                io.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.ENTRADA_SALIDA);
                cpu.retirar();
                usadosQuantum = 0;
                bloqueados.agregar(io);
                log.agregar(ciclo, id, io.getId(), "BLOQUEO_ES", "Hasta ciclo " + io.getEsHasta());
            }
            if (cpu.getActual() == null) usadosQuantum = 0;
            politica.ordenar(listos);
            registrarUso(ciclo);
            admitirPendientes();
            ultimoCiclo = ciclo;
        } finally { cicloEnCurso = -1; mutex.release(); }
    }

    /**
     * Instruccion en modo sistema: el proceso pide su operacion CRUD. Si la identidad del
     * proceso no tiene permiso, o la operacion no es posible, se rechaza con una explicacion
     * en el log y la simulacion sigue.
     * @return true si la solicitud quedo en la cola del disco
     */
    private boolean solicitarArchivo(Proceso proceso, long ciclo) {
        proceso.ejecutarInstruccion();
        ParametrosArchivo datos = datosArchivo(proceso);
        try {
            PlanOperacionArchivo plan = sistemaArchivos.planificar(proceso.getOperacionES(),
                    proceso.getRutaObjetivo(), datos);
            long solicitud = disco.encolarSolicitud(proceso.getId(), proceso.getOperacionES(),
                    plan.bloqueObjetivo(), plan.bloquesTrabajo(), ciclo);
            proceso.registrarSolicitudDisco(solicitud);
            log.agregar(ciclo, id, proceso.getId(), "SOLICITUD_ARCHIVO", proceso.getOperacionES() + " "
                    + proceso.getRutaObjetivo() + " como " + datos.usuario() + ": S" + solicitud
                    + " al bloque " + plan.bloqueObjetivo() + " (" + plan.bloquesTrabajo() + " bloques)");
            return true;
        } catch (SecurityException | IllegalArgumentException | IllegalStateException error) {
            proceso.cerrarOperacionArchivo();
            log.agregar(ciclo, id, proceso.getId(), "RECHAZO_ARCHIVO", proceso.getOperacionES() + " "
                    + proceso.getRutaObjetivo() + " como " + datos.usuario() + ": " + error.getMessage());
            return false;
        }
    }

    /** Desbloquea a los procesos cuyo disco termino en un ciclo anterior y aplica su operacion. */
    private void atenderCompletadas(long ciclo) {
        if (disco == null) return;
        for (VistaSolicitudDisco completada : disco.tomarCompletadas(ciclo)) {
            if (completada.operacion() == OperacionES.SUSPENDER || completada.operacion() == OperacionES.REACTIVAR) {
                atenderSwap(completada, ciclo);
                continue;
            }
            Proceso proceso = buscarBloqueadoPorDisco(completada.procesoId(), completada.id());
            if (proceso == null) continue;
            ParametrosArchivo datos = datosArchivo(proceso);
            try {
                String resultado = sistemaArchivos.ejecutar(proceso.getOperacionES(),
                        proceso.getRutaObjetivo(), datos, proceso.getId());
                log.agregar(ciclo, id, proceso.getId(), "FIN_ARCHIVO", "S" + completada.id() + " " + resultado);
            } catch (SecurityException | IllegalArgumentException | IllegalStateException error) {
                log.agregar(ciclo, id, proceso.getId(), "RECHAZO_ARCHIVO", "S" + completada.id()
                        + " no se pudo aplicar: " + error.getMessage());
            }
            proceso.cerrarOperacionArchivo();
            if (proceso.getRestante() == 0) {
                terminarRegistrado(proceso);
                continue;
            }
            despertar(proceso, ciclo, proceso.getEstado() == EstadoProceso.BLOQUEADO ? null : "FIN_DISCO",
                    "S" + completada.id() + " termino mientras estaba en swap");
        }
    }

    private Proceso buscarBloqueadoPorDisco(long procesoId, long solicitud) {
        for (Object elemento : concatenar(bloqueados.instantanea(), bloqueadosSuspendidos.instantanea())) {
            Proceso proceso = (Proceso) elemento;
            if (proceso.getId() == procesoId && proceso.getSolicitudDisco() == solicitud
                    && proceso.getMotivoBloqueo() == MotivoBloqueo.DISCO)
                return proceso;
        }
        return null;
    }

    private static ParametrosArchivo datosArchivo(Proceso proceso) {
        return proceso.getParametrosArchivo() == null ? ParametrosArchivo.ADMINISTRADOR
                : proceso.getParametrosArchivo();
    }

    private void finalizarES(long ciclo) {
        for (Object elemento : concatenar(bloqueados.instantanea(), bloqueadosSuspendidos.instantanea())) {
            Proceso proceso = (Proceso) elemento;
            if (proceso.getMotivoBloqueo() == MotivoBloqueo.ENTRADA_SALIDA && proceso.getEsHasta() <= ciclo)
                despertar(proceso, ciclo, "FIN_ES", "Termino su E/S");
        }
    }

    private static Object[] concatenar(Object[] primero, Object[] segundo) {
        Object[] todo = new Object[primero.length + segundo.length];
        System.arraycopy(primero, 0, todo, 0, primero.length);
        System.arraycopy(segundo, 0, todo, primero.length, segundo.length);
        return todo;
    }

    private void registrarUso(long ciclo) {
        int indice = (historiaInicio + historiaTamano) % historiaCiclos.length;
        if (historiaTamano == historiaCiclos.length) {
            indice = historiaInicio;
            historiaInicio = (historiaInicio + 1) % historiaCiclos.length;
        } else historiaTamano++;
        historiaCiclos[indice] = ciclo;
        historiaUso[indice] = 100.0 * ocupados / ciclo;
    }

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

    public void cambiarPolitica(PoliticaPlanificacion tipo, int nuevoQuantum) {
        if (tipo == null || nuevoQuantum < 1)
            throw new IllegalArgumentException("Politica y quantum positivo obligatorios");
        mutex.acquireUninterruptibly();
        try {
            politica = switch (tipo) {
                case FCFS -> new FCFS();
                case ROUND_ROBIN -> new Planificacion.RoundRobin();
                case EDF -> new Planificacion.EDF();
                case PRIORIDADES -> new Planificacion.Prioridades();
            };
            tipoPolitica = tipo;
            quantum = nuevoQuantum;
            usadosQuantum = 0;
            politica.ordenar(listos);
            log.agregar(ultimoCiclo, id, 0, "POLITICA", tipo + " quantum=" + nuevoQuantum);
        } finally { mutex.release(); }
    }

    public VistaComputador consultarVista() {
        mutex.acquireUninterruptibly();
        try {
            int[] ram = memoria.instantanea();
            Proceso actual = cpu.getActual();
            return new VistaComputador(id, ultimoCiclo, cpu.getModo(), ram[0], ram[1],
                    actual == null ? "Sin proceso" : describirProceso(actual),
                    describirCola(nuevos), describirCola(listos), describirCola(bloqueados), describirCola(terminados),
                    describirCola(listosSuspendidos), describirCola(bloqueadosSuspendidos),
                    actual == null ? null : vistaProceso(actual), vistasCola(nuevos), vistasCola(listos),
                    vistasCola(bloqueados), vistasCola(terminados), vistasCola(listosSuspendidos),
                    vistasCola(bloqueadosSuspendidos), tipoPolitica, quantum);
        } finally { mutex.release(); }
    }

    private Modelo.VistaProceso vistaProceso(Proceso proceso) {
        return new Modelo.VistaProceso(proceso.getId(), proceso.getNombre(), proceso.getMemoria(),
                proceso.getPrioridad(), proceso.getDeadline(), proceso.getRestante(), proceso.getPc(),
                proceso.getMar(), proceso.getEstado(), proceso.getTipo(), proceso.getMotivoBloqueo(), describirOperacion(proceso));
    }

    private String describirOperacion(Proceso proceso) {
        if (proceso.getTipo() != TipoProceso.IO_BOUND) return "";
        if (proceso.getOperacionES() != null) {
            String texto = proceso.getOperacionES() + (proceso.getRutaObjetivo() == null ? "" : " " + proceso.getRutaObjetivo());
            if (proceso.getParametrosArchivo() != null) texto += " como " + proceso.getParametrosArchivo().usuario();
            if (proceso.getSolicitudDisco() > 0 && !proceso.isOperacionAtendida())
                texto += " (esperando S" + proceso.getSolicitudDisco() + ")";
            else if (proceso.isOperacionAtendida()) texto += " (atendida)";
            return texto + describirSwap(proceso);
        }
        ParametrosES es = proceso.getParametrosES();
        if (es == null) return "E/S sin configurar";
        return "E/S rafaga=" + es.rafaga() + " duracion=" + es.duracion() + " hasta=" + proceso.getEsHasta()
                + describirSwap(proceso);
    }

    private static String describirSwap(Proceso proceso) {
        if (proceso.getBloqueSwap() < 0) return "";
        String texto = " | swap desde bloque " + proceso.getBloqueSwap();
        if (proceso.getSolicitudSwap() == 0) return texto;
        return texto + (proceso.isReactivando() ? " (leyendo S" : " (escribiendo S") + proceso.getSolicitudSwap() + ")";
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
        return "P" + proceso.getId() + " " + proceso.getNombre() + " | " + proceso.getTipo() + " | "
                + proceso.getEstado() + " | prioridad=" + proceso.getPrioridad() + " | RAM=" + proceso.getMemoria()
                + " | deadline=" + proceso.getDeadline() + " | restante=" + proceso.getRestante()
                + " | PC=" + proceso.getPc() + " | MAR=" + proceso.getMar() + " | bloqueo="
                + proceso.getMotivoBloqueo() + " | " + describirOperacion(proceso);
    }

    public VistaMetricas consultarMetricas() {
        mutex.acquireUninterruptibly();
        try {
            int completos = 0, vencidos = 0, cancelados = 0, respondidos = 0;
            double respuesta = 0, suma = 0, cuadrados = 0;
            Object[] procesos = todos.instantanea();
            for (Object elemento : procesos) {
                Proceso proceso = (Proceso) elemento;
                switch (proceso.getResultado()) {
                    case "COMPLETADO" -> completos++;
                    case "VENCIDO" -> vencidos++;
                    case "CANCELADO" -> cancelados++;
                }
                if (proceso.getRespuesta() >= 0) { respondidos++; respuesta += proceso.getRespuesta(); }
                suma += proceso.getPc();
                cuadrados += (double) proceso.getPc() * proceso.getPc();
            }
            long[] ciclos = new long[historiaTamano];
            double[] uso = new double[historiaTamano];
            for (int i = 0; i < ciclos.length; i++) {
                int indice = (historiaInicio + i) % historiaCiclos.length;
                ciclos[i] = historiaCiclos[indice];
                uso[i] = historiaUso[indice];
            }
            double media = respondidos == 0 ? 0 : respuesta / respondidos;
            double equidad = cuadrados == 0 ? 0 : suma * suma / (procesos.length * cuadrados);
            return new VistaMetricas(id, ultimoCiclo, ocupados, procesos.length, completos, vencidos,
                    cancelados, respondidos, media, equidad, suma, cuadrados, ciclos, uso);
        } finally { mutex.release(); }
    }

    private void retirar(Cola<Proceso> cola, long idProceso) {
        int cantidad = cola.getTamano();
        for (int i = 0; i < cantidad; i++) {
            Proceso proceso = cola.quitar();
            if (proceso.getId() != idProceso) cola.agregar(proceso);
        }
    }

    private boolean contiene(Cola<Proceso> cola, long idProceso) {
        for (Object elemento : cola.instantanea())
            if (((Proceso) elemento).getId() == idProceso) return true;
        return false;
    }

    private Proceso[] copiarProcesos(Cola<Proceso> cola) {
        mutex.acquireUninterruptibly();
        try {
            Object[] elementos = cola.instantanea();
            Proceso[] copia = new Proceso[elementos.length];
            for (int i = 0; i < elementos.length; i++) copia[i] = (Proceso) elementos[i];
            return copia;
        } finally { mutex.release(); }
    }

    public Proceso getProcesoEnCPU() { mutex.acquireUninterruptibly(); try { return cpu.getActual(); } finally { mutex.release(); } }
    public ModoCPU getModoCPU() { mutex.acquireUninterruptibly(); try { return cpu.getModo(); } finally { mutex.release(); } }
    public long getUltimoCiclo() { mutex.acquireUninterruptibly(); try { return ultimoCiclo; } finally { mutex.release(); } }
    public PoliticaPlanificacion getPolitica() { mutex.acquireUninterruptibly(); try { return tipoPolitica; } finally { mutex.release(); } }
    public int getQuantum() { mutex.acquireUninterruptibly(); try { return quantum; } finally { mutex.release(); } }
    public EventoSistema[] consultarEventos() { return log.consultar(); }
    public int getId() { return id; }
    public Memoria getMemoria() { return memoria; }
    public Proceso[] getNuevos() { return copiarProcesos(nuevos); }
    public Proceso[] getListos() { return copiarProcesos(listos); }
    public Proceso[] getBloqueados() { return copiarProcesos(bloqueados); }
    public Proceso[] getTerminados() { return copiarProcesos(terminados); }
    public Proceso[] getListosSuspendidos() { return copiarProcesos(listosSuspendidos); }
    public Proceso[] getBloqueadosSuspendidos() { return copiarProcesos(bloqueadosSuspendidos); }
}
