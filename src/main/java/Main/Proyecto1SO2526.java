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
