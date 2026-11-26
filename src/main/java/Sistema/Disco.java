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
    /** Completadas que el computador todavia no ha recogido para desbloquear a su proceso. */
    private final Cola<SolicitudDisco> porNotificar = new Cola<>();
    /**
     * Eventos que pide el hilo del computador para un ciclo que el disco todavia no cerro. Se
     * escriben al final de ese ciclo del disco, asi el log no depende de que hilo corrio primero.
     */
    private final Cola<Modelo.EventoSistema> porRegistrar = new Cola<>();
    /** Ciclo que corre el computador, segun sus ultimas llamadas; fecha los cambios de bloques. */
    private long cicloCliente;
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
        try { return encolar(procesoId, operacion, bloqueObjetivo, bloquesTrabajo, ultimoCiclo); }
        finally { mutex.release(); }
    }

    /**
     * Encola una solicitud que llega en {@code cicloLlegada}. El disco la puede empezar a
     * atender desde el ciclo siguiente, aunque el computador y el disco corran el mismo ciclo
     * en paralelo: asi el resultado no depende de que hilo llego primero.
     */
    public long encolarSolicitud(long procesoId, OperacionES operacion,
            int bloqueObjetivo, int bloquesTrabajo, long cicloLlegada) {
        mutex.acquireUninterruptibly();
        try {
            if (cicloLlegada < ultimoCiclo)
                throw new IllegalArgumentException("La solicitud no puede llegar en un ciclo pasado");
            return encolar(procesoId, operacion, bloqueObjetivo, bloquesTrabajo, cicloLlegada);
        } finally { mutex.release(); }
    }

    private long encolar(long procesoId, OperacionES operacion, int bloqueObjetivo,
            int bloquesTrabajo, long cicloLlegada) {
        if (procesoId < 1 || operacion == null || bloqueObjetivo < 0
                || bloqueObjetivo >= bloques.length || bloquesTrabajo < 1)
            throw new IllegalArgumentException("Solicitud de disco invalida");
        AreaDisco areaEsperada = operacion.requiereRuta() ? AreaDisco.DATOS : AreaDisco.SWAP;
        if (bloques[bloqueObjetivo].getArea() != areaEsperada)
            throw new IllegalArgumentException("La operacion no corresponde al area del bloque");
        if (siguienteSolicitud == Long.MAX_VALUE)
            throw new IllegalStateException("Se agotaron los identificadores de solicitudes");
        SolicitudDisco solicitud = new SolicitudDisco(siguienteSolicitud++, procesoId,
                operacion, bloqueObjetivo, bloquesTrabajo, cicloLlegada);
        pendientes.agregar(solicitud);
        cicloCliente = Math.max(cicloCliente, cicloLlegada);
        registrarExterno(cicloLlegada, procesoId, "SOLICITUD_DISCO",
                "S" + solicitud.getId() + " " + operacion + " bloque=" + bloqueObjetivo);
        return solicitud.getId();
    }

    /**
     * Entrega y olvida las solicitudes que terminaron antes de {@code antesDe}. El computador
     * la llama en el ciclo c para desbloquear a los procesos cuyo disco termino hasta c-1.
     */
    public VistaSolicitudDisco[] tomarCompletadas(long antesDe) {
        mutex.acquireUninterruptibly();
        try {
            cicloCliente = Math.max(cicloCliente, antesDe);
            int cantidad = porNotificar.getTamano();
            Cola<SolicitudDisco> listas = new Cola<>();
            for (int i = 0; i < cantidad; i++) {
                SolicitudDisco solicitud = porNotificar.quitar();
                if (solicitud.getCicloFin() < antesDe) listas.agregar(solicitud);
                else porNotificar.agregar(solicitud);
            }
            return vistas(listas);
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
                    activa.finalizar(ciclo);
                    registrarFinalizacion(activa, ciclo);
                    completadas.agregar(activa);
                    porNotificar.agregar(activa);
                    activa = null;
                    rutaActiva = new MovimientoCabezalDisco[0];
                    indiceRuta = 0;
                }
            }
            registrarHistoria(ciclo);
            registrarPendientes(ciclo);
            ultimoCiclo = ciclo;
        } finally { mutex.release(); }
    }

    /** Anota un evento del computador ahora o, si su ciclo sigue abierto en el disco, al cerrarlo. */
    private void registrarExterno(long ciclo, long proceso, String tipo, String detalle) {
        if (ciclo <= ultimoCiclo) log.agregar(ciclo, 1, proceso, tipo, detalle);
        else porRegistrar.agregar(new Modelo.EventoSistema(0, ciclo, 1, proceso, tipo, detalle));
    }

    private void registrarPendientes(long ciclo) {
        int cantidad = porRegistrar.getTamano();
        for (int i = 0; i < cantidad; i++) {
            Modelo.EventoSistema evento = porRegistrar.quitar();
            if (evento.ciclo() <= ciclo)
                log.agregar(evento.ciclo(), 1, evento.proceso(), evento.tipo(), evento.detalle());
            else porRegistrar.agregar(evento);
        }
    }

    private long cicloBloques() { return Math.max(ultimoCiclo, cicloCliente); }

    private void prepararSiguiente(long ciclo) {
        Object[] cola = pendientes.instantanea();
        int disponibles = 0;
        for (Object elemento : cola) if (((SolicitudDisco) elemento).disponibleEn(ciclo)) disponibles++;
        if (disponibles == 0) return;
        int[] objetivos = new int[disponibles];
        int indice = 0;
        for (Object elemento : cola) {
            SolicitudDisco solicitud = (SolicitudDisco) elemento;
            if (solicitud.disponibleEn(ciclo)) objetivos[indice++] = solicitud.getBloqueObjetivo();
        }
        ResultadoPlanificacionDisco plan = politica.planificar(
                posicionCabezal, bloques.length, direccion, objetivos);
        MovimientoCabezalDisco[] recorrido = plan.movimientos();
        int fin = 0;
        while (fin < recorrido.length && !recorrido[fin].atiendeSolicitud()) fin++;
        if (fin == recorrido.length) throw new IllegalStateException("La politica no selecciono una solicitud");
        rutaActiva = new MovimientoCabezalDisco[fin + 1];
        System.arraycopy(recorrido, 0, rutaActiva, 0, rutaActiva.length);
        indiceRuta = 0;
        activa = retirarPrimeraEnBloque(recorrido[fin].destino(), ciclo);
        activa.iniciar(politica.getTipo(), ciclo);
        solicitudesIniciadas++;
        esperaAcumulada += activa.getTiempoEspera();
        log.agregar(ciclo, 1, activa.getProcesoId(), "DISCO_ATIENDE",
                "S" + activa.getId() + " bloque=" + activa.getBloqueObjetivo()
                + " (" + activa.getPoliticaSeleccion() + ")");
    }

    private SolicitudDisco retirarPrimeraEnBloque(int bloque, long ciclo) {
        SolicitudDisco elegida = null;
        int cantidad = pendientes.getTamano();
        for (int i = 0; i < cantidad; i++) {
            SolicitudDisco solicitud = pendientes.quitar();
            if (elegida == null && solicitud.disponibleEn(ciclo)
                    && solicitud.getBloqueObjetivo() == bloque) elegida = solicitud;
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
            registrarExterno(cicloBloques(), 0, "ASIGNA_BLOQUES",
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
            registrarExterno(cicloBloques(), 0, "ASIGNA_BLOQUES",
                    propietario + " " + cantidad + " bloques desde " + primerBloque);
            return primerBloque;
        } finally { mutex.release(); }
    }

    /**
     * Restaura una cadena guardada con sus bloques exactos, en orden de enlace. Todos deben
     * ser del area de datos, estar libres y no repetirse; si alguno falla, no asigna nada.
     * @return numero del primer bloque de la cadena
     */
    public int asignarCadena(String propietario, int[] cadena) {
        if (propietario == null || propietario.isBlank() || cadena == null || cadena.length == 0)
            throw new IllegalArgumentException("Asignacion invalida");
        int[] elegidos = cadena.clone();
        mutex.acquireUninterruptibly();
        try {
            for (int i = 0; i < elegidos.length; i++) {
                int numero = elegidos[i];
                if (numero < 0 || numero >= bloques.length || bloques[numero].getArea() != AreaDisco.DATOS)
                    throw new IllegalArgumentException("El bloque " + numero + " no es del area de datos");
                if (!bloques[numero].estaLibre())
                    throw new IllegalStateException("El bloque " + numero + " ya esta ocupado por "
                            + bloques[numero].getPropietario());
                for (int j = 0; j < i; j++)
                    if (elegidos[j] == numero)
                        throw new IllegalArgumentException("El bloque " + numero + " se repite en la cadena");
            }
            for (int i = 0; i < elegidos.length; i++)
                bloques[elegidos[i]].ocupar(propietario, i + 1 < elegidos.length ? elegidos[i + 1] : -1);
            registrarExterno(cicloBloques(), 0, "ASIGNA_BLOQUES",
                    propietario + " " + elegidos.length + " bloques desde " + elegidos[0]);
            return elegidos[0];
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
            registrarExterno(cicloBloques(), 0, "LIBERA_BLOQUES",
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

    /** Primer bloque libre del area, en orden de numero; -1 si el area esta llena. */
    public int primerLibre(AreaDisco area) {
        if (area == null) throw new IllegalArgumentException("El area es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            for (int numero = 0; numero < bloques.length; numero++)
                if (bloques[numero].getArea() == area && bloques[numero].estaLibre()) return numero;
            return -1;
        } finally { mutex.release(); }
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
