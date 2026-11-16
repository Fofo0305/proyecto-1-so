package Sistema;

import EDD.Cola;
import Modelo.EstadoProceso;
import Modelo.EventoSistema;
import Modelo.Memoria;
import Modelo.ModoCPU;
import Modelo.MotivoBloqueo;
import Modelo.ParametrosES;
import Modelo.PoliticaPlanificacion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.VistaMetricas;
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

    public Computador(int id, int capacidadRam) {
        if (id != 1) throw new IllegalArgumentException("AvilaOS utiliza un unico computador con ID 1");
        this.id = id;
        memoria = new Memoria(capacidadRam);
    }

    public void recibirProceso(Proceso proceso) {
        if (proceso == null || proceso.getComputador() != id || proceso.getEstado() != EstadoProceso.NUEVO)
            throw new IllegalArgumentException("El proceso debe ser nuevo y pertenecer al computador");
        mutex.acquireUninterruptibly();
        try {
            if (contiene(nuevos, proceso.getId()) || contiene(listos, proceso.getId())
                    || contiene(bloqueados, proceso.getId()) || contiene(terminados, proceso.getId())
                    || cpu.getActual() == proceso)
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

    private int admitirPendientes() {
        int admitidos = 0;
        int pendientes = nuevos.getTamano();
        for (int i = 0; i < pendientes; i++) {
            Proceso proceso = nuevos.primero();
            if (memoria.reservar("proceso:" + proceso.getId(), proceso.getMemoria())) {
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
        if (!contiene(nuevos, proceso.getId()) && !contiene(listos, proceso.getId())
                && !contiene(bloqueados, proceso.getId()) && cpu.getActual() != proceso)
            throw new IllegalArgumentException("El proceso no esta registrado en el computador");
        proceso.cerrarResultado();
        log.agregar(cicloEnCurso >= 0 ? cicloEnCurso : ultimoCiclo, id, proceso.getId(), "TERMINACION", proceso.getResultado());
        proceso.cambiarEstado(EstadoProceso.TERMINADO, MotivoBloqueo.NINGUNO);
        retirar(nuevos, proceso.getId());
        retirar(listos, proceso.getId());
        retirar(bloqueados, proceso.getId());
        if (cpu.getActual() == proceso) { cpu.retirar(); usadosQuantum = 0; }
        memoria.liberar("proceso:" + proceso.getId());
        terminados.agregar(proceso);
        return true;
    }

    @Override
    public void avanzarCiclo(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (ciclo != ultimoCiclo + 1) throw new IllegalArgumentException("El ciclo debe ser consecutivo");
            cicloEnCurso = ciclo;
            admitirPendientes();
            Object[][] colasCiclo = {nuevos.instantanea(), listos.instantanea(), bloqueados.instantanea()};
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
            cpu.avanzarCiclo(ciclo);
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

    private void finalizarES(long ciclo) {
        for (Object elemento : bloqueados.instantanea()) {
            Proceso proceso = (Proceso) elemento;
            if (proceso.getMotivoBloqueo() == MotivoBloqueo.ENTRADA_SALIDA && proceso.getEsHasta() <= ciclo) {
                retirar(bloqueados, proceso.getId());
                proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
                listos.agregar(proceso);
                log.agregar(ciclo, id, proceso.getId(), "FIN_ES", "Vuelve a listos");
            }
        }
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
                    actual == null ? null : vistaProceso(actual), vistasCola(nuevos), vistasCola(listos),
                    vistasCola(bloqueados), vistasCola(terminados), tipoPolitica, quantum);
        } finally { mutex.release(); }
    }

    private Modelo.VistaProceso vistaProceso(Proceso proceso) {
        return new Modelo.VistaProceso(proceso.getId(), proceso.getNombre(), proceso.getMemoria(),
                proceso.getPrioridad(), proceso.getDeadline(), proceso.getRestante(), proceso.getPc(),
                proceso.getMar(), proceso.getEstado(), proceso.getTipo(), proceso.getMotivoBloqueo(), describirOperacion(proceso));
    }

    private String describirOperacion(Proceso proceso) {
        if (proceso.getTipo() != TipoProceso.IO_BOUND) return "";
        ParametrosES es = proceso.getParametrosES();
        return "E/S rafaga=" + es.rafaga() + " duracion=" + es.duracion() + " hasta=" + proceso.getEsHasta();
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
}
