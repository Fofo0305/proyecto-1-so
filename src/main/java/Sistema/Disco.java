package Sistema;

import EDD.Cola;
import Modelo.AreaDisco;
import Modelo.DireccionDisco;
import Modelo.MovimientoCabezalDisco;
import Modelo.OperacionES;
import Modelo.PoliticaDisco;
import Modelo.ResultadoPlanificacionDisco;
import Modelo.VistaBloqueDisco;
import Modelo.VistaMetricasDisco;
import Modelo.VistaSolicitudDisco;
import PlanificacionDisco.FIFO;
import PlanificacionDisco.PoliticaPlanificacionDisco;
import java.util.concurrent.Semaphore;

/** Mapa del unico disco local de AvilaOS. @author Adolfo */
public final class Disco implements ComponenteReloj {
    public static final int MINIMO_BLOQUES = 100;
    public static final int MAXIMO_BLOQUES = 500;

    private final Semaphore mutex = new Semaphore(1, true);
    private final BloqueDisco[] bloques;
    private final int tamanoSwap;
    private final int posicionesPorCiclo;
    private final Cola<SolicitudDisco> pendientes = new Cola<>();
    private final Cola<SolicitudDisco> completadas = new Cola<>();
    private final RegistroSistema log = new RegistroSistema();
    private final long[] atendidasPorPolitica = new long[PoliticaDisco.values().length];
    private final long[] historiaCiclos = new long[600];
    private final long[] historiaDesplazamiento = new long[600];
    private int posicionCabezal;
    private DireccionDisco direccion = DireccionDisco.ASCENDENTE;
    private PoliticaPlanificacionDisco politica = new FIFO();
    private SolicitudDisco activa;
    private MovimientoCabezalDisco[] rutaActiva = new MovimientoCabezalDisco[0];
    private int indiceRuta;
    private long siguienteSolicitud = 1;
    private long ultimoCiclo;
    private long desplazamientoTotal;
    private long solicitudesIniciadas;
    private long solicitudesAtendidas;
    private long esperaAcumulada;
    private long suspensiones;
    private long reactivaciones;
    private int historiaInicio;
    private int historiaTamano;

    public Disco(int totalBloques, int bloquesSwap, int cabezalInicial) {
        this(totalBloques, bloquesSwap, cabezalInicial, 1);
    }

    public Disco(int totalBloques, int bloquesSwap, int cabezalInicial, int posicionesPorCiclo) {
        if (totalBloques < MINIMO_BLOQUES || totalBloques > MAXIMO_BLOQUES)
            throw new IllegalArgumentException("El disco debe tener entre 100 y 500 bloques");
        if (bloquesSwap < 1 || bloquesSwap >= totalBloques)
            throw new IllegalArgumentException("El area swap debe ocupar una parte no vacia del disco");
        if (cabezalInicial < 0 || cabezalInicial >= totalBloques)
            throw new IllegalArgumentException("El cabezal inicial esta fuera del disco");
        if (posicionesPorCiclo < 1)
            throw new IllegalArgumentException("El cabezal debe avanzar al menos una posicion por ciclo");
        tamanoSwap = bloquesSwap;
        this.posicionesPorCiclo = posicionesPorCiclo;
        posicionCabezal = cabezalInicial;
        bloques = new BloqueDisco[totalBloques];
        int inicioSwap = totalBloques - bloquesSwap;
        for (int numero = 0; numero < bloques.length; numero++)
            bloques[numero] = new BloqueDisco(numero, numero < inicioSwap ? AreaDisco.DATOS : AreaDisco.SWAP);
    }

    public long encolarSolicitud(long procesoId, OperacionES operacion,
            int bloqueObjetivo, int bloquesTrabajo) {
        mutex.acquireUninterruptibly();
        try {
            if (procesoId < 1 || operacion == null || bloqueObjetivo < 0
                    || bloqueObjetivo >= bloques.length || bloquesTrabajo < 1)
                throw new IllegalArgumentException("Solicitud de disco invalida");
            AreaDisco areaEsperada = operacion.requiereRuta() ? AreaDisco.DATOS : AreaDisco.SWAP;
            if (bloques[bloqueObjetivo].getArea() != areaEsperada)
                throw new IllegalArgumentException("La operacion no corresponde al area del bloque");
            if (siguienteSolicitud == Long.MAX_VALUE)
                throw new IllegalStateException("Se agotaron los identificadores de solicitudes");
            SolicitudDisco solicitud = new SolicitudDisco(siguienteSolicitud++, procesoId,
                    operacion, bloqueObjetivo, bloquesTrabajo, ultimoCiclo);
            pendientes.agregar(solicitud);
            log.agregar(ultimoCiclo, 1, procesoId, "SOLICITUD_DISCO",
                    "S" + solicitud.getId() + " " + operacion + " bloque=" + bloqueObjetivo);
            return solicitud.consultar().id();
        } finally { mutex.release(); }
    }

    public void cambiarPolitica(PoliticaPlanificacionDisco nuevaPolitica) {
        if (nuevaPolitica == null) throw new IllegalArgumentException("La politica es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            politica = nuevaPolitica;
            log.agregar(ultimoCiclo, 1, 0, "POLITICA_DISCO", nuevaPolitica.getTipo().name());
        }
        finally { mutex.release(); }
    }

    @Override public void avanzarCiclo(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (ciclo != ultimoCiclo + 1) throw new IllegalArgumentException("El ciclo debe ser consecutivo");
            if (activa == null) prepararSiguiente(ciclo);
            if (activa != null && !avanzarCabezal()) {
                activa.trabajarBloque();
                if (activa.termino()) {
                    registrarFinalizacion(activa, ciclo);
                    completadas.agregar(activa);
                    activa = null;
                    rutaActiva = new MovimientoCabezalDisco[0];
                    indiceRuta = 0;
                }
            }
            registrarHistoria(ciclo);
            ultimoCiclo = ciclo;
        } finally { mutex.release(); }
    }

    private void prepararSiguiente(long ciclo) {
        Object[] espera = pendientes.instantanea();
        if (espera.length == 0) return;
        int[] objetivos = new int[espera.length];
        for (int i = 0; i < espera.length; i++)
            objetivos[i] = ((SolicitudDisco) espera[i]).getBloqueObjetivo();
        ResultadoPlanificacionDisco plan = politica.planificar(
                posicionCabezal, bloques.length, direccion, objetivos);
        MovimientoCabezalDisco[] recorrido = plan.movimientos();
        int fin = 0;
        while (fin < recorrido.length && !recorrido[fin].atiendeSolicitud()) fin++;
        if (fin == recorrido.length) throw new IllegalStateException("La politica no selecciono una solicitud");
        rutaActiva = new MovimientoCabezalDisco[fin + 1];
        System.arraycopy(recorrido, 0, rutaActiva, 0, rutaActiva.length);
        indiceRuta = 0;
        activa = retirarPrimeraEnBloque(recorrido[fin].destino());
        activa.iniciar(politica.getTipo(), ciclo);
        solicitudesIniciadas++;
        esperaAcumulada += activa.getTiempoEspera();
        log.agregar(ciclo, 1, activa.getProcesoId(), "DISCO_ATIENDE",
                "S" + activa.getId() + " bloque=" + activa.getBloqueObjetivo()
                + " (" + activa.getPoliticaSeleccion() + ")");
    }

    private SolicitudDisco retirarPrimeraEnBloque(int bloque) {
        SolicitudDisco elegida = null;
        int cantidad = pendientes.getTamano();
        for (int i = 0; i < cantidad; i++) {
            SolicitudDisco solicitud = pendientes.quitar();
            if (elegida == null && solicitud.getBloqueObjetivo() == bloque) elegida = solicitud;
            else pendientes.agregar(solicitud);
        }
        if (elegida == null) throw new IllegalStateException("La politica eligio una solicitud inexistente");
        return elegida;
    }

    /** @return true cuando el ciclo se consumio moviendo fisicamente el cabezal. */
    private boolean avanzarCabezal() {
        while (indiceRuta < rutaActiva.length) {
            MovimientoCabezalDisco movimiento = rutaActiva[indiceRuta];
            if (!movimiento.contabilizaDesplazamiento()) {
                posicionCabezal = movimiento.destino();
                indiceRuta++;
                continue;
            }
            if (posicionCabezal == movimiento.destino()) {
                indiceRuta++;
                continue;
            }
            int diferencia = movimiento.destino() - posicionCabezal;
            direccion = diferencia > 0 ? DireccionDisco.ASCENDENTE : DireccionDisco.DESCENDENTE;
            int avance = Math.min(Math.abs(diferencia), posicionesPorCiclo);
            posicionCabezal += diferencia > 0 ? avance : -avance;
            desplazamientoTotal += avance;
            if (posicionCabezal == movimiento.destino()) indiceRuta++;
            return true;
        }
        return false;
    }

    private void registrarFinalizacion(SolicitudDisco solicitud, long ciclo) {
        solicitudesAtendidas++;
        atendidasPorPolitica[solicitud.getPoliticaSeleccion().ordinal()]++;
        if (solicitud.getOperacion() == OperacionES.SUSPENDER) suspensiones++;
        else if (solicitud.getOperacion() == OperacionES.REACTIVAR) reactivaciones++;
        log.agregar(ciclo, 1, solicitud.getProcesoId(), "DISCO_COMPLETA",
                "S" + solicitud.getId() + " " + solicitud.getOperacion());
    }

    private void registrarHistoria(long ciclo) {
        int indice = (historiaInicio + historiaTamano) % historiaCiclos.length;
        if (historiaTamano == historiaCiclos.length) {
            indice = historiaInicio;
            historiaInicio = (historiaInicio + 1) % historiaCiclos.length;
        } else historiaTamano++;
        historiaCiclos[indice] = ciclo;
        historiaDesplazamiento[indice] = desplazamientoTotal;
    }

    public VistaSolicitudDisco[] consultarCola() {
        mutex.acquireUninterruptibly();
        try { return vistas(pendientes); }
        finally { mutex.release(); }
    }

    public VistaSolicitudDisco[] consultarCompletadas() {
        mutex.acquireUninterruptibly();
        try { return vistas(completadas); }
        finally { mutex.release(); }
    }

    public VistaSolicitudDisco consultarActiva() {
        mutex.acquireUninterruptibly();
        try { return activa == null ? null : activa.consultar(); }
        finally { mutex.release(); }
    }

    public VistaMetricasDisco consultarMetricas() {
        mutex.acquireUninterruptibly();
        try {
            long[] ciclos = new long[historiaTamano];
            long[] desplazamientos = new long[historiaTamano];
            for (int i = 0; i < historiaTamano; i++) {
                int indice = (historiaInicio + i) % historiaCiclos.length;
                ciclos[i] = historiaCiclos[indice];
                desplazamientos[i] = historiaDesplazamiento[indice];
            }
            double esperaPromedio = solicitudesIniciadas == 0 ? 0
                    : (double) esperaAcumulada / solicitudesIniciadas;
            return new VistaMetricasDisco(ultimoCiclo, desplazamientoTotal,
                    solicitudesAtendidas, esperaPromedio, suspensiones, reactivaciones,
                    atendidasPorPolitica, ciclos, desplazamientos);
        } finally { mutex.release(); }
    }

    public Modelo.EventoSistema[] consultarEventos() { return log.consultar(); }

    private VistaSolicitudDisco[] vistas(Cola<SolicitudDisco> cola) {
        Object[] elementos = cola.instantanea();
        VistaSolicitudDisco[] vistas = new VistaSolicitudDisco[elementos.length];
        for (int i = 0; i < vistas.length; i++) vistas[i] = ((SolicitudDisco) elementos[i]).consultar();
        return vistas;
    }

    /**
     * Asigna por primer ajuste {@code cantidad} bloques libres del area de datos y los
     * encadena: cada bloque guarda el numero del siguiente y el ultimo guarda -1.
     * @return numero del primer bloque de la cadena
     */
    public int asignarEncadenado(String propietario, int cantidad) {
        if (propietario == null || propietario.isBlank() || cantidad < 1)
            throw new IllegalArgumentException("Asignacion invalida");
        mutex.acquireUninterruptibly();
        try {
            if (libresEn(AreaDisco.DATOS) < cantidad)
                throw new IllegalStateException("No hay " + cantidad + " bloques libres en el disco");
            int[] elegidos = new int[cantidad];
            int encontrados = 0;
            for (int numero = 0; numero < bloques.length && encontrados < cantidad; numero++)
                if (bloques[numero].getArea() == AreaDisco.DATOS && bloques[numero].estaLibre())
                    elegidos[encontrados++] = numero;
            for (int i = 0; i < cantidad; i++)
                bloques[elegidos[i]].ocupar(propietario, i + 1 < cantidad ? elegidos[i + 1] : -1);
            log.agregar(ultimoCiclo, 1, 0, "ASIGNA_BLOQUES",
                    propietario + " " + cantidad + " bloques desde " + elegidos[0]);
            return elegidos[0];
        } finally { mutex.release(); }
    }

    /**
     * Restaura una cadena guardada: empieza exactamente en {@code primerBloque} y sigue con
     * los bloques libres del area de datos que vienen despues, en orden. Si no caben todos,
     * no asigna nada.
     * @return numero del primer bloque de la cadena
     */
    public int asignarDesde(String propietario, int primerBloque, int cantidad) {
        if (propietario == null || propietario.isBlank() || cantidad < 1)
            throw new IllegalArgumentException("Asignacion invalida");
        mutex.acquireUninterruptibly();
        try {
            if (primerBloque < 0 || primerBloque >= bloques.length
                    || bloques[primerBloque].getArea() != AreaDisco.DATOS)
                throw new IllegalArgumentException("El bloque " + primerBloque + " no es del area de datos");
            if (!bloques[primerBloque].estaLibre())
                throw new IllegalStateException("El bloque " + primerBloque + " ya esta ocupado por "
                        + bloques[primerBloque].getPropietario());
            int[] elegidos = new int[cantidad];
            int encontrados = 0;
            for (int numero = primerBloque; numero < bloques.length && encontrados < cantidad; numero++)
                if (bloques[numero].getArea() == AreaDisco.DATOS && bloques[numero].estaLibre())
                    elegidos[encontrados++] = numero;
            if (encontrados < cantidad)
                throw new IllegalStateException("No hay " + cantidad + " bloques libres desde el bloque "
                        + primerBloque);
            for (int i = 0; i < cantidad; i++)
                bloques[elegidos[i]].ocupar(propietario, i + 1 < cantidad ? elegidos[i + 1] : -1);
            log.agregar(ultimoCiclo, 1, 0, "ASIGNA_BLOQUES",
                    propietario + " " + cantidad + " bloques desde " + primerBloque);
            return primerBloque;
        } finally { mutex.release(); }
    }

    /** Bloques de la cadena que empieza en {@code primerBloque}, en orden de enlace. */
    public int[] recorrerCadena(int primerBloque) {
        mutex.acquireUninterruptibly();
        try { return cadena(primerBloque); }
        finally { mutex.release(); }
    }

    /** Libera toda la cadena que empieza en {@code primerBloque}. @return bloques liberados */
    public int liberarEncadenado(int primerBloque) {
        mutex.acquireUninterruptibly();
        try {
            int[] recorrido = cadena(primerBloque);
            String propietario = bloques[primerBloque].getPropietario();
            for (int numero : recorrido) bloques[numero].liberar();
            log.agregar(ultimoCiclo, 1, 0, "LIBERA_BLOQUES",
                    propietario + " " + recorrido.length + " bloques desde " + primerBloque);
            return recorrido.length;
        } finally { mutex.release(); }
    }

    public int contarLibres(AreaDisco area) {
        if (area == null) throw new IllegalArgumentException("El area es obligatoria");
        mutex.acquireUninterruptibly();
        try { return libresEn(area); }
        finally { mutex.release(); }
    }

    private int libresEn(AreaDisco area) {
        int libres = 0;
        for (BloqueDisco bloque : bloques) if (bloque.getArea() == area && bloque.estaLibre()) libres++;
        return libres;
    }

    private int[] cadena(int primerBloque) {
        if (primerBloque < 0 || primerBloque >= bloques.length || bloques[primerBloque].estaLibre())
            throw new IllegalArgumentException("El bloque " + primerBloque + " no inicia una cadena");
        String propietario = bloques[primerBloque].getPropietario();
        int largo = 0;
        for (int actual = primerBloque; actual != -1; actual = bloques[actual].getSiguiente()) {
            if (++largo > bloques.length || bloques[actual].estaLibre()
                    || !bloques[actual].getPropietario().equals(propietario))
                throw new IllegalStateException("La cadena del bloque " + primerBloque + " esta corrupta");
        }
        int[] recorrido = new int[largo];
        int actual = primerBloque;
        for (int i = 0; i < largo; i++) {
            recorrido[i] = actual;
            actual = bloques[actual].getSiguiente();
        }
        return recorrido;
    }

    public VistaBloqueDisco[] consultarBloques() {
        mutex.acquireUninterruptibly();
        try {
            VistaBloqueDisco[] copia = new VistaBloqueDisco[bloques.length];
            for (int i = 0; i < copia.length; i++) copia[i] = bloques[i].consultar();
            return copia;
        } finally { mutex.release(); }
    }

    public int getTotalBloques() { return bloques.length; }
    public int getTamanoSwap() { return tamanoSwap; }
    public int getPosicionesPorCiclo() { return posicionesPorCiclo; }

    public long getUltimoCiclo() {
        mutex.acquireUninterruptibly();
        try { return ultimoCiclo; }
        finally { mutex.release(); }
    }

    public PoliticaDisco getPolitica() {
        mutex.acquireUninterruptibly();
        try { return politica.getTipo(); }
        finally { mutex.release(); }
    }

    public int getPosicionCabezal() {
        mutex.acquireUninterruptibly();
        try { return posicionCabezal; }
        finally { mutex.release(); }
    }

    public DireccionDisco getDireccion() {
        mutex.acquireUninterruptibly();
        try { return direccion; }
        finally { mutex.release(); }
    }
}
