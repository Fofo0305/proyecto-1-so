package Pruebas;

import Sistema.ControladorSimulacion;
import Modelo.*;
import Persistencia.ArchivoConfiguracion;
import java.nio.file.Path;
/** Escenarios de RAM y persistencia de buffers. @author Luciano Rojas */
public class PruebaRegistroBuffers {
    static void verificar(boolean valor) { if (!valor) throw new AssertionError("No se cumple una condicion del registro de buffers"); }
    static void rechazar(Runnable accion) { try { accion.run(); throw new AssertionError(); } catch (IllegalArgumentException esperado) {} }
    public static void main(String[] args) throws Exception {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.configurar(new Configuracion(new int[]{10,20},1000,4,2));
            Buffer<Object> b = c.crearBuffer("Datos",6,2);
            verificar(b.getAnfitrion()==2 && c.getComputadores()[0].getMemoria().getUsada()==0);
            verificar(c.getComputadores()[1].getMemoria().getUsada()==6 && c.getBuffers().length==1);
            rechazar(() -> c.crearBuffer("Datos",1,1));
            rechazar(() -> c.crearBuffer("Grande",15,2));
            rechazar(() -> c.crearBuffer("Invalido",1,3));
            rechazar(() -> c.crearBuffer("Sin capacidad",0,1));
            rechazar(() -> c.crearBuffer("Sin anfitrion",1,0));
            rechazar(() -> c.crearBuffer(" ",1,1));
            verificar(c.getBuffers().length==1 && c.getComputadores()[1].getMemoria().getUsada()==6);
            Buffer<?>[] copia=c.getBuffers(); copia[0]=null; verificar(c.getBuffers()[0]==b);
            c.crearProceso("CPU",2,1,14,1,10);
            c.crearProceso("Espera",2,1,1,1,10);
            verificar(c.getComputadores()[1].getNuevos().length==1);
            c.getComputadores()[1].avanzarCiclo(1);
            verificar(c.getComputadores()[1].getMemoria().getUsada()==7);
            Path archivo=java.nio.file.Files.createTempFile("avilaos-buffers-", ".csv");
            ArchivoConfiguracion.guardar(archivo,c.getCargaInicial());
            try { c.cargarCarga(ArchivoConfiguracion.cargar(archivo)); } finally { java.nio.file.Files.deleteIfExists(archivo); }
            verificar(c.getBuffers().length==1 && c.getBuffers()[0].getCantidad()==0);
            verificar(c.getCargaInicial().getBuffers()[0].capacidad()==6);
            c.configurar(new Configuracion(new int[]{10,20},1000,4,0));
            verificar(c.getBuffers().length==0 && c.getComputadores()[1].getMemoria().getUsada()==0);
            c.cargarCarga(Ejemplos.ConfiguracionInicial.crear());
            verificar(c.getBuffers().length==1 && c.getBuffers()[0].getNombre().equals("Datos"));
            Thread primero = new Thread(() -> {
                try { c.crearBuffer("Unico",1,2); } catch (IllegalArgumentException esperado) { }
            });
            Thread segundo = new Thread(() -> {
                try { c.crearBuffer("Unico",1,2); } catch (IllegalArgumentException esperado) { }
            });
            primero.start(); segundo.start(); primero.join(); segundo.join();
            verificar(c.getBuffers().length==2 && c.getComputadores()[1].getMemoria().getUsada()==81);
            c.detener();
            try { c.crearBuffer("Detenido",1,1); throw new AssertionError(); } catch (IllegalStateException esperado) {}
        }
        System.out.println("OK: registro, RAM anfitriona, rechazos sin cambios, admision, CSV, ejemplo y reinicio");
    }
}
