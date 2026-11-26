package Pruebas;

import Ejemplos.ArchivosEnunciado;
import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;
import Modelo.OperacionES;
import Modelo.PoliticaDisco;
import Modelo.PoliticaPlanificacion;
import Persistencia.ArchivoProcesos;
import Sistema.ControladorSimulacion;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Contrato del JSON de procesos: lee el ejemplo del enunciado, rechaza datos imposibles con
 * un mensaje claro, no deja iniciar hasta tener los dos JSON y conserva el disco modificado
 * cuando se vuelven a cargar los procesos.
 * @author Luciano
 */
public final class PruebaArchivoProcesos {
    private PruebaArchivoProcesos() { }

    public static void main(String[] args) throws Exception {
        probarJsonDelEnunciado();
        probarErrores();
        probarConstantesIgualesALosArchivos();
        probarCargaEnControlador();
        System.out.println("OK: JSON de procesos, puerta de Iniciar y disco conservado al recargar");
    }

    private static void probarJsonDelEnunciado() throws Exception {
        CargaInicial carga = ArchivoProcesos.cargar(Path.of("ejemplos", "procesos.json"));
        Configuracion conf = carga.getConfiguracion();
        verificar(conf.getRam()[0] == 1024 && conf.getDuracionMs() == 300, "No respeta ramCapacity ni cycleDurationMs");
        verificar(conf.getPoliticas()[0] == PoliticaPlanificacion.FCFS, "La politica del computador no es FCFS");
        verificar(conf.getQuantum() == ArchivoProcesos.QUANTUM_POR_DEFECTO, "Un quantum 0 con FCFS debe tomar el valor por defecto");
        verificar(conf.getBloquesSwap() == 100 && conf.getPoliticaDisco() == PoliticaDisco.FIFO,
                "No respeta swapSizeBlocks ni initialDiskPolicy");
        DefinicionProceso[] procesos = carga.getProcesos();
        verificar(procesos.length == 8, "No cargo los ocho procesos del enunciado");
        verificar(procesos[0].nombre().equals("Proc_95") && procesos[0].operacionES() == OperacionES.LEER
                && procesos[0].rutaObjetivo().equals("/sistema/config.sys"), "El primer proceso no es READ de config.sys");
        verificar(procesos[2].operacionES() == OperacionES.ACTUALIZAR && procesos[3].operacionES() == OperacionES.ELIMINAR,
                "UPDATE o DELETE no se tradujeron");
        verificar(procesos[0].memoria() == 16 && procesos[0].instrucciones() == 2 && procesos[0].deadline() == 500,
                "Memoria, instrucciones o deadline no coinciden");

        CargaInicial minima = ArchivoProcesos.leer("{\"computer\":{\"ramCapacity\":64,\"policy\":\"round-robin\",\"quantum\":3},"
                + "\"processes\":[{\"name\":\"A\",\"memory\":8,\"instructions\":5,\"deadline\":40},"
                + "{\"name\":\"B\",\"memory\":8,\"instructions\":2,\"deadline\":40,\"targetPath\":\"/n.txt\","
                + "\"operation\":\"CREATE\",\"fileSizeBlocks\":3,\"permissions\":\"private\"}]}");
        Configuracion otra = minima.getConfiguracion();
        verificar(otra.getDuracionMs() == ArchivoProcesos.CICLO_POR_DEFECTO && otra.getQuantum() == 3
                && otra.getPoliticas()[0] == PoliticaPlanificacion.ROUND_ROBIN, "Sin globalConfig no usa los valores por defecto");
        verificar(minima.getProcesos()[0].operacionES() == null && minima.getProcesos()[1].operacionES() == OperacionES.CREAR
                && minima.getProcesos()[1].parametrosArchivo().tamanoBloques() == 3, "CPU o CREATE mal leidos");
    }

    private static void probarErrores() {
        rechaza("{\"computer\":{\"ramCapacity\":64,\"policy\":\"RR\",\"quantum\":0},\"processes\":[]}",
                "Round Robin necesita un quantum de al menos 1");
        rechaza("{\"computer\":{\"ramCapacity\":64},\"processes\":[{\"name\":\"C\",\"memory\":8,\"instructions\":2,"
                + "\"deadline\":9,\"targetPath\":\"/c.txt\",\"operation\":\"CREATE\",\"fileSizeBlocks\":0}]}",
                "C: CREATE necesita fileSizeBlocks de al menos 1");
        rechaza("{\"computer\":{\"ramCapacity\":64},\"processes\":[{\"name\":\"G\",\"memory\":65,\"instructions\":2,\"deadline\":9}]}",
                "G: pide 65 de memoria y la RAM es de 64");
        rechaza("{\"computer\":{\"ramCapacity\":64},\"processes\":[{\"name\":\"R\",\"memory\":8,\"instructions\":2,"
                + "\"deadline\":9,\"targetPath\":\"/r.txt\"}]}", "targetPath y operation van juntos");
        rechaza("{\"processes\":[]}", "computer");
        rechaza("{no es json", "JSON de procesos invalido");
    }

    private static void probarConstantesIgualesALosArchivos() throws Exception {
        verificar(JsonParser.parseString(ArchivosEnunciado.PROCESOS)
                .equals(JsonParser.parseString(leer("procesos.json"))), "ArchivosEnunciado.PROCESOS difiere del archivo");
        verificar(JsonParser.parseString(ArchivosEnunciado.SISTEMA_ARCHIVOS)
                .equals(JsonParser.parseString(leer("sistema-archivos.json"))), "ArchivosEnunciado.SISTEMA_ARCHIVOS difiere del archivo");
    }

    private static void probarCargaEnControlador() throws Exception {
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            verificar("Configure primero la simulacion".equals(controlador.faltaParaIniciar()), "Sin simulacion no lo dice");
            controlador.configurar(new Configuracion(new int[] {128}, 10, 4));
            verificar("Faltan el JSON del disco y el JSON de procesos".equals(controlador.faltaParaIniciar()),
                    "No pide los dos JSON");
            controlador.cargarSistemaArchivos(leer("sistema-archivos.json"));
            verificar("Falta cargar el JSON de procesos".equals(controlador.faltaParaIniciar()), "No pide el JSON de procesos");
            controlador.cargarProcesos(leer("procesos.json"));
            verificar(controlador.faltaParaIniciar() == null, "Con los dos JSON aun dice que falta algo");

            verificar(controlador.getDisco().getTotalBloques() == 200 && controlador.getDisco().getPosicionCabezal() == 50
                    && controlador.getDisco().getTamanoSwap() == 19, "El disco no quedo como dice el JSON del disco");
            verificar(controlador.getConfiguracion().getRam()[0] == 1024 && controlador.getCargaInicial().getProcesos().length == 8,
                    "La configuracion no salio del JSON de procesos");

            controlador.cambiarDuracion(5);
            controlador.iniciar();
            esperar(() -> !controlador.getSistemaArchivos().existe("/usuarios/img1.png"), "Proc_11 no borro img1.png");
            controlador.detener();

            controlador.cargarProcesos(leer("procesos.json"));
            verificar(!controlador.getSistemaArchivos().existe("/usuarios/img1.png"),
                    "Recargar los procesos devolvio el archivo borrado");
            verificar(controlador.faltaParaIniciar() == null, "Despues de recargar no deja iniciar");
            controlador.configurar(new Configuracion(new int[] {128}, 10, 4));
            verificar("Falta cargar el JSON de procesos".equals(controlador.faltaParaIniciar()),
                    "Aplicar una configuracion a mano no vuelve a pedir el JSON de procesos");
        }
    }

    private static void rechaza(String json, String fragmento) {
        try {
            ArchivoProcesos.leer(json);
        } catch (IllegalArgumentException error) {
            verificar(error.getMessage().contains(fragmento), "Mensaje inesperado: " + error.getMessage());
            return;
        }
        throw new AssertionError("Acepto un JSON que debia rechazar: " + fragmento);
    }

    private static String leer(String nombre) throws Exception {
        return Files.readString(Path.of("ejemplos", nombre), StandardCharsets.UTF_8);
    }

    private static void esperar(Callable<Boolean> condicion, String que) throws Exception {
        long limite = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < limite) {
            if (condicion.call()) return;
            Thread.sleep(20);
        }
        throw new AssertionError("No se cumplio: " + que);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
