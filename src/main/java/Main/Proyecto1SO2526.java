/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package Main;

/**
 *
 * @author Adolfo
 */
public class Proyecto1SO2526 {

    public static void main(String[] args) {
        System.out.println("Hello World!");

        /* Prueba de reservas de RAM verificada en consola.
        Modelo.Memoria ramReservas = new Modelo.Memoria(100);
        boolean primeraReserva = ramReservas.reservar(60);
        System.out.println("Reserva 60: " + primeraReserva + ", usada/libre: " + ramReservas.getUsada() + "/" + ramReservas.getLibre() + " (esperado: true, 60/40)");
        if (!primeraReserva || ramReservas.getUsada() != 60 || ramReservas.getLibre() != 40)
            throw new AssertionError("Reserva inicial incorrecta");
        boolean sinEspacio = ramReservas.reservar(41);
        System.out.println("Reserva sin espacio: " + sinEspacio + ", usada: " + ramReservas.getUsada() + " (esperado: false, 60)");
        if (sinEspacio || ramReservas.getUsada() != 60) throw new AssertionError("Reserva fallida altero RAM");
        if (!ramReservas.reservar(40) || ramReservas.getLibre() != 0 || ramReservas.reservar(1))
            throw new AssertionError("Limite de capacidad incorrecto");
        for (int cantidadInvalida : new int[] {0, -1, Integer.MIN_VALUE}) {
            try {
                ramReservas.reservar(cantidadInvalida);
                throw new AssertionError("Se acepto reserva no positiva");
            } catch (IllegalArgumentException esperado) {
                if (ramReservas.getUsada() != 100) throw new AssertionError("Rechazo altero RAM");
            }
        }
        Modelo.Memoria ramGrande = new Modelo.Memoria(Integer.MAX_VALUE);
        if (!ramGrande.reservar(Integer.MAX_VALUE) || ramGrande.reservar(1) || ramGrande.getLibre() != 0)
            throw new AssertionError("Desbordamiento en reserva");
        System.out.println("RAM llena, cantidades invalidas y limite entero: OK");
        System.out.println("Prueba de reservas de RAM: OK");
        */

        /* Prueba de capacidad de RAM verificada en consola.
        Modelo.Memoria ramInicial = new Modelo.Memoria(256);
        System.out.println("RAM capacidad/usada/libre: " + ramInicial.getCapacidad() + "/" + ramInicial.getUsada() + "/" + ramInicial.getLibre() + " (esperado: 256/0/256)");
        if (ramInicial.getCapacidad() != 256 || ramInicial.getUsada() != 0 || ramInicial.getLibre() != 256)
            throw new AssertionError("RAM inicial incorrecta");
        Modelo.Memoria ramMinima = new Modelo.Memoria(1);
        if (ramMinima.getCapacidad() != 1 || ramMinima.getLibre() != 1)
            throw new AssertionError("Capacidad minima incorrecta");
        for (int capacidadInvalida : new int[] {0, -1, Integer.MIN_VALUE}) {
            try {
                new Modelo.Memoria(capacidadInvalida);
                throw new AssertionError("Se acepto RAM no positiva");
            } catch (IllegalArgumentException esperado) {
                System.out.println("Capacidad rechazada: " + capacidadInvalida);
            }
        }
        System.out.println("Prueba de capacidad y consultas de RAM: OK");
        */

        /* Prueba integrada del modelo verificada en consola.
        EDD.Cola<Modelo.Proceso> cargaModelo = new EDD.Cola<>();
        Modelo.Proceso completo = new Modelo.Proceso("Completo", 1, Modelo.TipoProceso.CPU_BOUND, 0, 32, 2, 2);
        Modelo.Proceso vencido = new Modelo.Proceso("Vencido", 2, Modelo.TipoProceso.CPU_BOUND, 1, 16, 3, 1);
        Modelo.Proceso bloqueadoModelo = new Modelo.Proceso("Bloqueado", 2, Modelo.TipoProceso.IO_BOUND, 2, 16, 3, 3);
        cargaModelo.agregar(completo);
        cargaModelo.agregar(vencido);
        cargaModelo.agregar(bloqueadoModelo);
        if (cargaModelo.quitar() != completo || cargaModelo.quitar() != vencido || cargaModelo.quitar() != bloqueadoModelo)
            throw new AssertionError("La cola altero el orden de los PCB");
        completo.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        completo.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        completo.ejecutarInstruccion();
        completo.descontarDeadline();
        completo.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        completo.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        completo.ejecutarInstruccion();
        completo.descontarDeadline();
        if (completo.getRestante() == 0)
            completo.cambiarEstado(Modelo.EstadoProceso.TERMINADO, Modelo.MotivoBloqueo.NINGUNO);
        else if (completo.getDeadline() == 0)
            throw new AssertionError("Se priorizo vencimiento sobre finalizacion");
        if (completo.getEstado() != Modelo.EstadoProceso.TERMINADO || completo.getPc() != 2
                || completo.getMar() != 2 || completo.getDeadline() != 0 || completo.getRestante() != 0)
            throw new AssertionError("Finalizacion integrada incorrecta");
        System.out.println("Finalizacion coincidente con deadline: TERMINADO, PC/MAR 2/2, restante/plazo 0/0");
        vencido.descontarDeadline();
        if (vencido.getDeadline() == 0)
            vencido.cambiarEstado(Modelo.EstadoProceso.TERMINADO, Modelo.MotivoBloqueo.NINGUNO);
        if (vencido.getRestante() != 3 || vencido.getPc() != 0 || vencido.getEstado() != Modelo.EstadoProceso.TERMINADO)
            throw new AssertionError("Vencimiento en nuevos incorrecto");
        System.out.println("Vencimiento en NUEVO: TERMINADO sin ejecutar instrucciones");
        bloqueadoModelo.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        bloqueadoModelo.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        bloqueadoModelo.ejecutarInstruccion();
        bloqueadoModelo.descontarDeadline();
        bloqueadoModelo.cambiarEstado(Modelo.EstadoProceso.BLOQUEADO, Modelo.MotivoBloqueo.ENTRADA_SALIDA);
        bloqueadoModelo.descontarDeadline();
        bloqueadoModelo.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        if (bloqueadoModelo.getComputador() != 2 || bloqueadoModelo.getPc() != 1
                || bloqueadoModelo.getMar() != 1 || bloqueadoModelo.getRestante() != 2
                || bloqueadoModelo.getDeadline() != 1 || bloqueadoModelo.getMotivoBloqueo() != Modelo.MotivoBloqueo.NINGUNO)
            throw new AssertionError("Despertar perdio el contexto");
        System.out.println("Bloqueo y retorno a LISTO: contexto conservado, motivo NINGUNO");
        if (completo.getId() == vencido.getId() || completo.getId() == bloqueadoModelo.getId()
                || vencido.getId() == bloqueadoModelo.getId()) throw new AssertionError("IDs de carga repetidos");
        System.out.println("Prueba integrada del modelo de procesos: OK");
        */

        /* Prueba de deadline verificada en consola.
        Modelo.Proceso plazo = new Modelo.Proceso("Plazo", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 5, 2);
        plazo.descontarDeadline();
        System.out.println("Deadline en NUEVO: " + plazo.getDeadline() + " (esperado: 1)");
        if (plazo.getDeadline() != 1) throw new AssertionError("No desconto en nuevos");
        plazo.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        plazo.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        plazo.cambiarEstado(Modelo.EstadoProceso.BLOQUEADO, Modelo.MotivoBloqueo.RED);
        plazo.descontarDeadline();
        plazo.descontarDeadline();
        System.out.println("Deadline en BLOQUEADO sin negativos: " + plazo.getDeadline() + " (esperado: 0)");
        if (plazo.getDeadline() != 0 || plazo.getDeadlineInicial() != 2) throw new AssertionError("Deadline incorrecto");
        plazo.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        plazo.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        try {
            plazo.ejecutarInstruccion();
            throw new AssertionError("Proceso vencido ejecuto");
        } catch (IllegalStateException esperado) {
            if (plazo.getPc() != 0 || plazo.getMar() != 0 || plazo.getRestante() != 5)
                throw new AssertionError("Proceso vencido alterado");
        }
        Modelo.Proceso finPlazo = new Modelo.Proceso("Fin", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 1, 3);
        finPlazo.cambiarEstado(Modelo.EstadoProceso.TERMINADO, Modelo.MotivoBloqueo.NINGUNO);
        finPlazo.descontarDeadline();
        System.out.println("Deadline en TERMINADO: " + finPlazo.getDeadline() + " (esperado: 3)");
        if (finPlazo.getDeadline() != 3) throw new AssertionError("Terminado perdio deadline");
        System.out.println("Prueba de deadline y rechazo de ejecucion vencida: OK");
        */

        /* Prueba de ejecucion y contexto verificada en consola.
        Modelo.Proceso ejecutable = new Modelo.Proceso("CPU", 1, Modelo.TipoProceso.CPU_BOUND, 0, 16, 2, 5);
        try {
            ejecutable.ejecutarInstruccion();
            throw new AssertionError("Un proceso nuevo ejecuto");
        } catch (IllegalStateException esperado) {
            if (ejecutable.getPc() != 0 || ejecutable.getMar() != 0 || ejecutable.getRestante() != 2)
                throw new AssertionError("El rechazo altero el contexto");
        }
        ejecutable.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        ejecutable.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        ejecutable.ejecutarInstruccion();
        System.out.println("Primera instruccion PC/MAR/restante: " + ejecutable.getPc() + "/" + ejecutable.getMar() + "/" + ejecutable.getRestante() + " (esperado: 1/1/1)");
        if (ejecutable.getPc() != 1 || ejecutable.getMar() != 1 || ejecutable.getRestante() != 1)
            throw new AssertionError("Primera instruccion incorrecta");
        ejecutable.cambiarEstado(Modelo.EstadoProceso.BLOQUEADO, Modelo.MotivoBloqueo.ENTRADA_SALIDA);
        try {
            ejecutable.ejecutarInstruccion();
            throw new AssertionError("Un bloqueado ejecuto");
        } catch (IllegalStateException esperado) {
            if (ejecutable.getPc() != 1 || ejecutable.getRestante() != 1) throw new AssertionError("Bloqueado alterado");
        }
        ejecutable.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
        ejecutable.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
        ejecutable.ejecutarInstruccion();
        try {
            ejecutable.ejecutarInstruccion();
            throw new AssertionError("Se ejecuto mas trabajo del declarado");
        } catch (IllegalStateException esperado) {
            if (ejecutable.getPc() != 2 || ejecutable.getMar() != 2 || ejecutable.getRestante() != 0)
                throw new AssertionError("Fin de instrucciones incorrecto");
        }
        Modelo.Proceso terminadoCpu = new Modelo.Proceso("Fin", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 1, 1);
        terminadoCpu.cambiarEstado(Modelo.EstadoProceso.TERMINADO, Modelo.MotivoBloqueo.NINGUNO);
        try {
            terminadoCpu.ejecutarInstruccion();
            throw new AssertionError("Un terminado ejecuto");
        } catch (IllegalStateException esperado) {
            if (terminadoCpu.getPc() != 0 || terminadoCpu.getRestante() != 1) throw new AssertionError("Terminado alterado");
        }
        System.out.println("Ejecucion completa PC/MAR/restante: 2/2/0; nuevos, bloqueados, terminados y trabajo agotado rechazados");
        System.out.println("Prueba de ejecucion y contexto: OK");
        */

        /* Prueba de transiciones y motivos verificada en consola.
        Modelo.EstadoProceso[] estadosPrueba = Modelo.EstadoProceso.values();
        int transicionesValidas = 0;
        int transicionesRechazadas = 0;
        for (Modelo.EstadoProceso origen : estadosPrueba) {
            for (Modelo.EstadoProceso destino : estadosPrueba) {
                Modelo.Proceso transicion = new Modelo.Proceso("T", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 2, 3);
                if (origen != Modelo.EstadoProceso.NUEVO) transicion.cambiarEstado(Modelo.EstadoProceso.LISTO, Modelo.MotivoBloqueo.NINGUNO);
                if (origen == Modelo.EstadoProceso.EJECUCION || origen == Modelo.EstadoProceso.BLOQUEADO)
                    transicion.cambiarEstado(Modelo.EstadoProceso.EJECUCION, Modelo.MotivoBloqueo.NINGUNO);
                if (origen == Modelo.EstadoProceso.BLOQUEADO)
                    transicion.cambiarEstado(origen, Modelo.MotivoBloqueo.SEMAFORO);
                if (origen == Modelo.EstadoProceso.TERMINADO)
                    transicion.cambiarEstado(origen, Modelo.MotivoBloqueo.NINGUNO);
                boolean esperado = switch (origen) {
                    case NUEVO -> destino == Modelo.EstadoProceso.LISTO || destino == Modelo.EstadoProceso.TERMINADO;
                    case LISTO -> destino == Modelo.EstadoProceso.EJECUCION || destino == Modelo.EstadoProceso.TERMINADO;
                    case EJECUCION -> destino == Modelo.EstadoProceso.LISTO || destino == Modelo.EstadoProceso.BLOQUEADO || destino == Modelo.EstadoProceso.TERMINADO;
                    case BLOQUEADO -> destino == Modelo.EstadoProceso.LISTO || destino == Modelo.EstadoProceso.TERMINADO;
                    case TERMINADO -> false;
                };
                Modelo.MotivoBloqueo motivoAnterior = transicion.getMotivoBloqueo();
                try {
                    transicion.cambiarEstado(destino, destino == Modelo.EstadoProceso.BLOQUEADO ? Modelo.MotivoBloqueo.RED : Modelo.MotivoBloqueo.NINGUNO);
                    if (!esperado || transicion.getEstado() != destino) throw new AssertionError("Transicion invalida aceptada");
                    transicionesValidas++;
                } catch (IllegalStateException rechazo) {
                    if (esperado || transicion.getEstado() != origen || transicion.getMotivoBloqueo() != motivoAnterior)
                        throw new AssertionError("Rechazo altero el contexto");
                    transicionesRechazadas++;
                }
            }
        }
        Modelo.Proceso motivoInvalido = new Modelo.Proceso("M", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 2, 3);
        for (int caso = 0; caso < 4; caso++) {
            try {
                motivoInvalido.cambiarEstado(caso == 0 ? null : caso == 3 ? Modelo.EstadoProceso.BLOQUEADO : Modelo.EstadoProceso.LISTO,
                        caso == 1 ? null : caso == 2 ? Modelo.MotivoBloqueo.RED : Modelo.MotivoBloqueo.NINGUNO);
                throw new AssertionError("Estado o motivo incoherente aceptado");
            } catch (IllegalArgumentException esperado) {
                if (motivoInvalido.getEstado() != Modelo.EstadoProceso.NUEVO || motivoInvalido.getMotivoBloqueo() != Modelo.MotivoBloqueo.NINGUNO)
                    throw new AssertionError("Motivo invalido altero contexto");
            }
        }
        System.out.println("Transiciones validas/rechazadas: " + transicionesValidas + "/" + transicionesRechazadas + " (esperado: 9/16)");
        System.out.println("Motivos incoherentes y nulos rechazados sin alterar contexto: OK");
        System.out.println("Prueba de transiciones: OK");
        */

        /* Prueba de validaciones del PCB verificada en consola.
        Modelo.Proceso antesValidacion = new Modelo.Proceso("Valido", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 1, 1);
        int rechazados = 0;
        for (int caso = 0; caso < 8; caso++) {
            try {
                new Modelo.Proceso(caso == 0 ? null : caso == 1 ? "  " : "P",
                        caso == 3 ? 0 : 1, caso == 2 ? null : Modelo.TipoProceso.CPU_BOUND,
                        caso == 4 ? -1 : 0, caso == 5 ? 0 : 1, caso == 6 ? 0 : 1, caso == 7 ? 0 : 1);
                throw new AssertionError("Se acepto el caso invalido " + caso);
            } catch (IllegalArgumentException esperado) {
                rechazados++;
                System.out.println("Caso invalido " + caso + ": rechazado");
            }
        }
        Modelo.Proceso despuesValidacion = new Modelo.Proceso("Valido", 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 1, 1);
        if (despuesValidacion.getId() != antesValidacion.getId() + 1 || rechazados != 8)
            throw new AssertionError("Validacion consumio IDs o no rechazo atributos");
        if (antesValidacion.getEstado() != Modelo.EstadoProceso.NUEVO || antesValidacion.getPc() != 0)
            throw new AssertionError("Se altero un proceso existente");
        System.out.println("Limites minimos validos y IDs sin consumir por errores: OK");
        System.out.println("Prueba de validaciones del PCB: OK");
        */

        /* Prueba de IDs globales concurrentes verificada en consola.
        long[][] ids = new long[4][100];
        Thread[] creadores = new Thread[ids.length];
        for (int i = 0; i < creadores.length; i++) {
            final int fila = i;
            creadores[i] = new Thread(() -> {
                for (int j = 0; j < ids[fila].length; j++) {
                    Modelo.Proceso procesoId = new Modelo.Proceso("P", fila + 1,
                            Modelo.TipoProceso.CPU_BOUND, 0, 16, 10, 20);
                    ids[fila][j] = procesoId.getId();
                }
            });
            creadores[i].start();
        }
        try {
            for (Thread creador : creadores) creador.join();
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de IDs interrumpida", interrupcion);
        }
        int cantidadIds = 0;
        for (int i = 0; i < 400; i++) {
            long actualId = ids[i / 100][i % 100];
            if (actualId <= 0) throw new AssertionError("ID no generado");
            for (int j = i + 1; j < 400; j++) {
                if (actualId == ids[j / 100][j % 100]) throw new AssertionError("ID repetido");
            }
            cantidadIds++;
        }
        System.out.println("IDs globales unicos entre cuatro computadores: " + cantidadIds + " (esperado: 400)");
        System.out.println("Prueba de IDs concurrentes: OK");
        */

        /* Prueba de PCB basico verificada en consola.
        Modelo.Proceso pcb = new Modelo.Proceso("P1", 2, Modelo.TipoProceso.CPU_BOUND, 3, 64, 10, 20);
        System.out.println("PCB: " + pcb.getNombre() + ", computador " + pcb.getComputador() + ", " + pcb.getTipo());
        System.out.println("Prioridad/RAM/instrucciones: " + pcb.getPrioridad() + "/" + pcb.getMemoria() + "/" + pcb.getInstrucciones());
        System.out.println("Estado: " + pcb.getEstado() + ", bloqueo: " + pcb.getMotivoBloqueo());
        System.out.println("PC/MAR/restante/deadline: " + pcb.getPc() + "/" + pcb.getMar() + "/" + pcb.getRestante() + "/" + pcb.getDeadline());
        if (!"P1".equals(pcb.getNombre()) || pcb.getComputador() != 2 || pcb.getTipo() != Modelo.TipoProceso.CPU_BOUND
                || pcb.getPrioridad() != 3 || pcb.getMemoria() != 64 || pcb.getInstrucciones() != 10
                || pcb.getEstado() != Modelo.EstadoProceso.NUEVO || pcb.getMotivoBloqueo() != Modelo.MotivoBloqueo.NINGUNO
                || pcb.getPc() != 0 || pcb.getMar() != 0 || pcb.getRestante() != 10
                || pcb.getDeadline() != 20 || pcb.getDeadlineInicial() != 20)
            throw new AssertionError("PCB inicial incorrecto");
        System.out.println("Prueba de PCB basico: OK");
        */

        /* Prueba de MotivoBloqueo verificada en consola.
        String[] nombresMotivos = {"NINGUNO", "ENTRADA_SALIDA", "SEMAFORO", "RED"};
        Modelo.MotivoBloqueo[] motivos = Modelo.MotivoBloqueo.values();
        if (motivos.length != nombresMotivos.length) throw new AssertionError("Cantidad de motivos incorrecta");
        for (int i = 0; i < motivos.length; i++) {
            System.out.println("Motivo: " + motivos[i] + " (esperado: " + nombresMotivos[i] + ")");
            if (!motivos[i].name().equals(nombresMotivos[i])
                    || Modelo.MotivoBloqueo.valueOf(nombresMotivos[i]) != motivos[i])
                throw new AssertionError("Motivo incorrecto");
        }
        System.out.println("Prueba de MotivoBloqueo: OK");
        */

        /* Prueba de PoliticaPlanificacion verificada en consola.
        String[] nombresPoliticas = {"FCFS", "ROUND_ROBIN", "EDF", "PRIORIDADES"};
        Modelo.PoliticaPlanificacion[] politicas = Modelo.PoliticaPlanificacion.values();
        if (politicas.length != nombresPoliticas.length) throw new AssertionError("Cantidad de politicas incorrecta");
        for (int i = 0; i < politicas.length; i++) {
            System.out.println("Politica: " + politicas[i] + " (esperado: " + nombresPoliticas[i] + ")");
            if (!politicas[i].name().equals(nombresPoliticas[i])
                    || Modelo.PoliticaPlanificacion.valueOf(nombresPoliticas[i]) != politicas[i])
                throw new AssertionError("Politica incorrecta");
        }
        System.out.println("Prueba de PoliticaPlanificacion: OK");
        */

        /* Prueba de ModoCPU verificada en consola.
        String[] nombresModos = {"USUARIO", "SISTEMA_OPERATIVO"};
        Modelo.ModoCPU[] modos = Modelo.ModoCPU.values();
        if (modos.length != nombresModos.length) throw new AssertionError("Cantidad de modos incorrecta");
        for (int i = 0; i < modos.length; i++) {
            System.out.println("Modo: " + modos[i] + " (esperado: " + nombresModos[i] + ")");
            if (!modos[i].name().equals(nombresModos[i])
                    || Modelo.ModoCPU.valueOf(nombresModos[i]) != modos[i])
                throw new AssertionError("Modo incorrecto");
        }
        System.out.println("Prueba de ModoCPU: OK");
        */

        /* Prueba de TipoProceso verificada en consola.
        String[] nombresTipos = {"CPU_BOUND", "IO_BOUND", "PRODUCTOR", "CONSUMIDOR"};
        Modelo.TipoProceso[] tipos = Modelo.TipoProceso.values();
        if (tipos.length != nombresTipos.length) throw new AssertionError("Cantidad de tipos incorrecta");
        for (int i = 0; i < tipos.length; i++) {
            System.out.println("Tipo: " + tipos[i] + " (esperado: " + nombresTipos[i] + ")");
            if (!tipos[i].name().equals(nombresTipos[i])
                    || Modelo.TipoProceso.valueOf(nombresTipos[i]) != tipos[i])
                throw new AssertionError("Tipo incorrecto");
        }
        System.out.println("Prueba de TipoProceso: OK");
        */

        /* Prueba de EstadoProceso verificada en consola.
        String[] nombresEstados = {"NUEVO", "LISTO", "EJECUCION", "BLOQUEADO", "TERMINADO"};
        Modelo.EstadoProceso[] estados = Modelo.EstadoProceso.values();
        if (estados.length != nombresEstados.length) throw new AssertionError("Cantidad de estados incorrecta");
        for (int i = 0; i < estados.length; i++) {
            System.out.println("Estado: " + estados[i] + " (esperado: " + nombresEstados[i] + ")");
            if (!estados[i].name().equals(nombresEstados[i])
                    || Modelo.EstadoProceso.valueOf(nombresEstados[i]) != estados[i])
                throw new AssertionError("Estado incorrecto");
        }
        System.out.println("Prueba de EstadoProceso: OK");
        */

        /* Prueba final integrada de EDD verificada en consola.
        EDD.Cola<Integer> integrada = new EDD.Cola<>();
        if (!integrada.estaVacia() || integrada.getTamano() != 0
                || integrada.quitar() != null || integrada.primero() != null
                || integrada.ultimo() != null || integrada.instantanea().length != 0)
            throw new AssertionError("Estado inicial incorrecto");
        System.out.println("Casos de cola vacia: OK");
        try {
            integrada.agregar(null);
            throw new AssertionError("Se acepto null");
        } catch (IllegalArgumentException esperado) {
            System.out.println("Rechazo de null: OK");
        }
        for (int vuelta = 0; vuelta < 3; vuelta++) {
            for (int i = 0; i < 100; i++) integrada.agregar(i);
            Object[] foto = integrada.instantanea();
            if (foto.length != 100 || integrada.primero() != 0 || integrada.ultimo() != 99)
                throw new AssertionError("Extremos o tamano incorrectos");
            for (int i = 0; i < 100; i++) {
                if (!Integer.valueOf(i).equals(foto[i]) || integrada.quitar() != i)
                    throw new AssertionError("Orden FIFO incorrecto");
            }
            if (!integrada.estaVacia() || integrada.getTamano() != 0 || integrada.ultimo() != null)
                throw new AssertionError("La cola no se vacio correctamente");
        }
        System.out.println("FIFO, instantaneas y reutilizacion en tres vueltas: OK");

        EDD.Cola<Integer> concurrente = new EDD.Cola<>();
        java.util.concurrent.Semaphore inicio = new java.util.concurrent.Semaphore(0);
        java.util.concurrent.Semaphore controlFinal = new java.util.concurrent.Semaphore(1);
        boolean[] recibidos = new boolean[2000];
        int[] conteos = new int[2];
        Thread[] trabajadores = new Thread[4];
        for (int i = 0; i < 2; i++) {
            final int base = i * 1000;
            trabajadores[i] = new Thread(() -> {
                inicio.acquireUninterruptibly();
                for (int j = 0; j < 1000; j++) concurrente.agregar(base + j);
            });
        }
        for (int i = 2; i < 4; i++) {
            trabajadores[i] = new Thread(() -> {
                inicio.acquireUninterruptibly();
                for (int intento = 0; intento < 5000; intento++) {
                    concurrente.instantanea();
                    Integer valor = concurrente.quitar();
                    if (valor == null) {
                        Thread.yield();
                        continue;
                    }
                    controlFinal.acquireUninterruptibly();
                    try {
                        if (valor < 0 || valor >= recibidos.length || recibidos[valor]) conteos[1]++;
                        else recibidos[valor] = true;
                        conteos[0]++;
                    } finally { controlFinal.release(); }
                }
            });
        }
        for (Thread trabajador : trabajadores) trabajador.start();
        inicio.release(4);
        try {
            for (Thread trabajador : trabajadores) trabajador.join();
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba final interrumpida", interrupcion);
        }
        Integer pendiente;
        while ((pendiente = concurrente.quitar()) != null) {
            if (pendiente < 0 || pendiente >= recibidos.length || recibidos[pendiente]) conteos[1]++;
            else recibidos[pendiente] = true;
            conteos[0]++;
        }
        System.out.println("Elementos recuperados: " + conteos[0] + ", duplicados o invalidos: " + conteos[1] + " (esperado: 2000, 0)");
        if (conteos[0] != 2000 || conteos[1] != 0 || !concurrente.estaVacia()
                || concurrente.getTamano() != 0 || concurrente.instantanea().length != 0)
            throw new AssertionError("Fallo de integracion concurrente");
        for (boolean recibido : recibidos)
            if (!recibido) throw new AssertionError("Elemento perdido");
        System.out.println("Prueba integrada de EDD: OK");
        */

        /* Prueba de instantanea verificada en consola.
        EDD.Cola<String> original = new EDD.Cola<>();
        System.out.println("Instantanea vacia: " + original.instantanea().length + " (esperado: 0)");
        if (original.instantanea().length != 0) throw new AssertionError("Copia vacia incorrecta");
        original.agregar("A");
        original.agregar("B");
        original.agregar("C");
        Object[] copia = original.instantanea();
        System.out.println("Copia FIFO: " + copia[0] + ", " + copia[1] + ", " + copia[2] + " (esperado: A, B, C)");
        if (copia.length != 3 || !"A".equals(copia[0]) || !"B".equals(copia[1]) || !"C".equals(copia[2]))
            throw new AssertionError("Orden de copia incorrecto");
        copia[0] = "X";
        System.out.println("Cola tras modificar arreglo: " + original.primero() + " (esperado: A)");
        if (!"A".equals(original.primero())) throw new AssertionError("Arreglo expone estructura interna");
        original.quitar();
        original.agregar("D");
        System.out.println("Copia previa tras modificar cola: " + copia[1] + ", " + copia[2] + " (esperado: B, C)");
        if (!"B".equals(copia[1]) || !"C".equals(copia[2])) throw new AssertionError("Copia alterada por cola");
        Object[] nueva = original.instantanea();
        System.out.println("Nueva copia: " + nueva[0] + ", " + nueva[1] + ", " + nueva[2] + " (esperado: B, C, D)");
        if (!"B".equals(nueva[0]) || !"C".equals(nueva[1]) || !"D".equals(nueva[2]))
            throw new AssertionError("Contenido actualizado incorrecto");

        */

        /* Prueba de semaforos y concurrencia verificada en consola.
        EDD.Cola<Integer> compartida = new EDD.Cola<>();
        Thread[] hilos = new Thread[4];
        for (int i = 0; i < hilos.length; i++) {
            final int inicio = i * 1000;
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    compartida.agregar(inicio + j);
                    compartida.primero();
                    compartida.ultimo();
                    compartida.getTamano();
                    compartida.estaVacia();
                }
            });
            hilos[i].start();
        }
        try {
            for (Thread hilo : hilos) hilo.join();
            System.out.println("Inserciones concurrentes: " + compartida.getTamano() + " (esperado: 4000)");
            if (compartida.getTamano() != 4000) throw new AssertionError("Se perdieron inserciones");
            boolean[] vistos = new boolean[4000];
            java.util.concurrent.Semaphore control = new java.util.concurrent.Semaphore(1);
            int[] resultados = new int[2];
            for (int i = 0; i < hilos.length; i++) {
                hilos[i] = new Thread(() -> {
                    Integer dato;
                    while ((dato = compartida.quitar()) != null) {
                        control.acquireUninterruptibly();
                        try {
                            if (dato < 0 || dato >= vistos.length || vistos[dato]) resultados[1]++;
                            else vistos[dato] = true;
                            resultados[0]++;
                        } finally { control.release(); }
                    }
                });
                hilos[i].start();
            }
            for (Thread hilo : hilos) hilo.join();
            System.out.println("Extracciones: " + resultados[0] + ", errores: " + resultados[1] + " (esperado: 4000, 0)");
            System.out.println("Cola final: " + compartida.getTamano() + ", " + compartida.estaVacia() + " (esperado: 0, true)");
            if (resultados[0] != 4000 || resultados[1] != 0 || !compartida.estaVacia())
                throw new AssertionError("Error de extraccion concurrente");
            try {
                compartida.agregar(null);
                throw new AssertionError("Se acepto null");
            } catch (IllegalArgumentException esperado) {
                compartida.agregar(7);
                System.out.println("Permiso tras excepcion: " + compartida.quitar() + " (esperado: 7)");
            }
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba interrumpida", interrupcion);
        }

        */

        /* Pruebas de Nodo y operaciones basicas de Cola verificadas en consola.
        EDD.Nodo<String> nodo = new EDD.Nodo<>("A");
        System.out.println("Nodo inicial: " + nodo.getDato() + " (esperado: A)");
        System.out.println("Siguiente inicial: " + nodo.getSiguiente() + " (esperado: null)");
        nodo.setDato("B");
        EDD.Nodo<String> siguiente = new EDD.Nodo<>("C");
        nodo.setSiguiente(siguiente);
        System.out.println("Dato actualizado: " + nodo.getDato() + " (esperado: B)");
        System.out.println("Dato siguiente: " + nodo.getSiguiente().getDato() + " (esperado: C)");
        nodo.setSiguiente(null);
        System.out.println("Enlace eliminado: " + nodo.getSiguiente() + " (esperado: null)");

        EDD.Cola<String> cola = new EDD.Cola<>();
        System.out.println("Cola inicial: " + cola.estaVacia() + ", " + cola.getTamano() + " (esperado: true, 0)");
        System.out.println("Extremos iniciales: " + cola.primero() + ", " + cola.ultimo() + " (esperado: null, null)");
        System.out.println("Quitar de vacia: " + cola.quitar() + " (esperado: null)");
        cola.agregar("A");
        System.out.println("Un elemento: " + cola.primero() + ", " + cola.ultimo() + ", " + cola.getTamano() + " (esperado: A, A, 1)");
        cola.agregar("B");
        cola.agregar("C");
        System.out.println("Extremos y tamano: " + cola.primero() + ", " + cola.ultimo() + ", " + cola.getTamano() + " (esperado: A, C, 3)");
        System.out.println("Consultas no retiran: " + cola.getTamano() + " (esperado: 3)");
        System.out.println("Orden FIFO: " + cola.quitar() + ", " + cola.quitar() + ", " + cola.quitar() + " (esperado: A, B, C)");
        System.out.println("Cola vaciada: " + cola.estaVacia() + ", " + cola.getTamano() + ", " + cola.primero() + ", " + cola.ultimo() + " (esperado: true, 0, null, null)");
        cola.agregar("D");
        System.out.println("Reutilizar cola: " + cola.primero() + ", " + cola.ultimo() + ", " + cola.quitar() + " (esperado: D, D, D)");
        try {
            cola.agregar(null);
            System.out.println("ERROR: se acepto null");
        } catch (IllegalArgumentException esperado) {
            System.out.println("Null rechazado correctamente");
        }
        System.out.println("Estado tras rechazo: " + cola.getTamano() + ", " + cola.estaVacia() + " (esperado: 0, true)");
        */
    }
}
