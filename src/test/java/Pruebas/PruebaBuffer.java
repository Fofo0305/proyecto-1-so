package Pruebas;

import Modelo.Buffer;

/** Comprobaciones ejecutables sin bibliotecas externas. @author Luciano Rojas */
public final class PruebaBuffer {
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    public static void main(String[] args) throws Exception {
        Buffer<Integer> buffer = new Buffer<>("Datos", 2, 1);
        verificar(buffer.extraer() == null, "Extraer de vacio debe devolver null");
        verificar(buffer.insertar(1) && buffer.insertar(2), "Deben caber dos elementos");
        verificar(!buffer.insertar(3), "No debe superar la capacidad");
        Object[] copia = buffer.instantanea();
        copia[0] = 99;
        verificar(buffer.extraer() == 1 && buffer.extraer() == 2, "Debe conservar FIFO y aislar la copia");
        verificar(buffer.insertar(3) && buffer.extraer() == 3, "Debe reutilizar espacio liberado");
        try {
            buffer.insertar(null);
            throw new AssertionError("Debe rechazar null");
        } catch (IllegalArgumentException esperado) { }
        verificar(buffer.estaVacio(), "Rechazar null no debe modificar el contenido");

        Buffer<Integer> compartido = new Buffer<>("Concurrente", 17, 1);
        java.util.concurrent.atomic.AtomicReference<Throwable> fallo =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread productor = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                while (!compartido.insertar(i)) {
                    if (Thread.currentThread().isInterrupted()) return;
                    Thread.yield();
                }
            }
        });
        Thread consumidor = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                Integer valor;
                while ((valor = compartido.extraer()) == null) {
                    if (Thread.currentThread().isInterrupted()) return;
                    Thread.yield();
                }
                verificar(valor == i, "FIFO incorrecto en acceso concurrente");
            }
        });
        productor.setUncaughtExceptionHandler((hilo, error) -> fallo.set(error));
        consumidor.setUncaughtExceptionHandler((hilo, error) -> fallo.set(error));
        productor.start();
        consumidor.start();
        productor.join(10000);
        consumidor.join(10000);
        boolean excedido = productor.isAlive() || consumidor.isAlive();
        productor.interrupt();
        consumidor.interrupt();
        productor.join(1000);
        consumidor.join(1000);
        verificar(!excedido, "Las transferencias excedieron el tiempo limite");
        if (fallo.get() != null) throw new AssertionError("Fallo concurrente", fallo.get());
        verificar(compartido.estaVacio(), "Quedaron elementos sin consumir");
        System.out.println("OK: capacidad, FIFO y 10000 transferencias concurrentes");
    }
}
