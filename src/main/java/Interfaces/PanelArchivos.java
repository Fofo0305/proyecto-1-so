package Interfaces;

import javax.swing.*;
import java.awt.*;

import Modelo.CargaInicial;
import Persistencia.ArchivoConfiguracion;
import Sistema.ControladorSimulacion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Selección de archivos en EDT y persistencia en un trabajador independiente. La segunda
 * fila carga los dos JSON que pide el enunciado antes de iniciar y dice cual falta.
 * @author Adolfo
 */
public class PanelArchivos extends JPanel {
    private final ControladorSimulacion controlador;
    private final java.util.function.Consumer<CargaInicial> alCargar;
    private final JButton abrirDisco = new JButton("Abrir JSON del disco");
    private final JButton abrirProcesos = new JButton("Abrir JSON de procesos");
    private final JButton guardarDisco = new JButton("Guardar JSON del disco");
    private final JLabel faltante = new JLabel();

    public PanelArchivos() { this(null, carga -> { }); }

    public PanelArchivos(ControladorSimulacion controlador, java.util.function.Consumer<CargaInicial> alCargar) {
        this.controlador = controlador;
        this.alCargar = alCargar;
        initComponents();
        guardar.addActionListener(evento -> seleccionar(true));
        cargar.addActionListener(evento -> seleccionar(false));
        ejemplo.addActionListener(evento -> cargarEjemplo());
        abrirDisco.addActionListener(evento -> elegirJson(Json.DISCO));
        abrirProcesos.addActionListener(evento -> elegirJson(Json.PROCESOS));
        guardarDisco.addActionListener(evento -> elegirJson(Json.GUARDAR_DISCO));
        ejemplo.setToolTipText("Carga los dos JSON del enunciado: el disco y los procesos");

        JPanel filaCsv = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel filaJson = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (Component componente : getComponents()) filaCsv.add(componente);
        removeAll();
        filaJson.add(abrirDisco);
        filaJson.add(abrirProcesos);
        filaJson.add(guardarDisco);
        filaJson.add(faltante);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        filaCsv.setAlignmentX(LEFT_ALIGNMENT);
        filaJson.setAlignmentX(LEFT_ALIGNMENT);
        add(filaJson);
        add(filaCsv);
        actualizarFaltante();
    }

    private enum Json { DISCO, PROCESOS, GUARDAR_DISCO }

    private void elegirJson(Json tipo) {
        JFileChooser selector = new JFileChooser(new java.io.File("ejemplos"));
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON", "json"));
        boolean escritura = tipo == Json.GUARDAR_DISCO;
        int eleccion = escritura ? selector.showSaveDialog(this) : selector.showOpenDialog(this);
        if (eleccion != JFileChooser.APPROVE_OPTION) return;
        Path ruta = selector.getSelectedFile().toPath();
        if (escritura && !ruta.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json"))
            ruta = ruta.resolveSibling(ruta.getFileName() + ".json");
        if (escritura && Files.exists(ruta)
                && JOptionPane.showConfirmDialog(this, "El archivo ya existe. Desea reemplazarlo?",
                        "Guardar JSON del disco", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        switch (tipo) {
            case DISCO -> cargarJsonDisco(ruta);
            case PROCESOS -> cargarJsonProcesos(ruta);
            case GUARDAR_DISCO -> guardarJsonDisco(ruta);
        }
    }

    /** Carga el JSON del disco (mapa de bloques, archivos y cabezal) en un trabajador. */
    public void cargarJsonDisco(Path ruta) {
        enTrabajador("Cargando " + ruta.getFileName() + "...", () -> {
            controlador.cargarSistemaArchivos(Files.readString(ruta, StandardCharsets.UTF_8));
            return "Disco cargado de " + ruta.getFileName() + ". " + describirDisco();
        });
    }

    /** Carga el JSON de procesos y configuracion; el disco que ya estaba se conserva. */
    public void cargarJsonProcesos(Path ruta) {
        enTrabajador("Cargando " + ruta.getFileName() + "...", () -> {
            controlador.cargarProcesos(Files.readString(ruta, StandardCharsets.UTF_8));
            return controlador.getCargaInicial().getProcesos().length + " procesos cargados de "
                    + ruta.getFileName() + ". " + describirDisco();
        });
    }

    /** Guarda el disco y el arbol tal como estan, para usarlos en otra simulacion. */
    public void guardarJsonDisco(Path ruta) {
        enTrabajador("Guardando " + ruta.getFileName() + "...", () -> {
            String json = controlador.exportarSistemaArchivos();
            if (json == null) throw new IllegalStateException("Configure primero la simulacion");
            Path temporal = ruta.resolveSibling(ruta.getFileName() + ".tmp");
            Files.writeString(temporal, json, StandardCharsets.UTF_8);
            Files.move(temporal, ruta, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "Disco guardado en " + ruta.getFileName();
        });
    }

    private interface Tarea { String hacer() throws java.io.IOException; }

    /** Corre la tarea fuera del EDT; al terminar avisa a la ventana si ya hay simulacion. */
    private void enTrabajador(String aviso, Tarea tarea) {
        if (controlador == null) { resultado.setText("Sin controlador"); return; }
        habilitar(false);
        resultado.setText(aviso);
        Thread trabajador = new Thread(() -> {
            String mensaje;
            CargaInicial carga = null;
            try {
                mensaje = tarea.hacer();
                if (controlador.getConfiguracion() != null) carga = controlador.getCargaInicial();
            } catch (java.io.IOException | RuntimeException error) {
                mensaje = "Error: " + error.getMessage();
            }
            String texto = mensaje;
            CargaInicial lista = carga;
            SwingUtilities.invokeLater(() -> {
                if (lista != null && !texto.startsWith("Error")) alCargar.accept(lista);
                resultado.setText(texto);
                habilitar(true);
            });
        }, "Archivo-json");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    /** Bloques, swap y cabezal del disco que quedo armado, para ver si el swap se recorto. */
    private String describirDisco() {
        Sistema.Disco disco = controlador.getDisco();
        Modelo.Configuracion configuracion = controlador.getConfiguracion();
        if (disco == null || configuracion == null) return "";
        String swap = disco.getTamanoSwap() + " de swap";
        if (disco.getTamanoSwap() < configuracion.getBloquesSwap())
            swap += " (se pidieron " + configuracion.getBloquesSwap() + "; no caben despues del ultimo archivo)";
        return "Disco: " + disco.getTotalBloques() + " bloques, " + swap + ", cabezal en " + disco.getPosicionCabezal() + ".";
    }

    /** Muestra que JSON falta para poder iniciar. */
    public void actualizarFaltante() {
        String falta = controlador == null ? "Sin controlador" : controlador.faltaParaIniciar();
        faltante.setText(falta == null ? "Listo: los dos JSON estan cargados" : falta);
        faltante.setForeground(falta == null ? new Color(30, 120, 60) : new Color(170, 70, 20));
    }

    private void seleccionar(boolean escritura) {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Configuracion CSV", "csv"));
        int eleccion = escritura ? selector.showSaveDialog(this) : selector.showOpenDialog(this);
        if (eleccion != JFileChooser.APPROVE_OPTION) return;
        Path ruta = selector.getSelectedFile().toPath();
        if (escritura && !ruta.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
            ruta = ruta.resolveSibling(ruta.getFileName() + ".csv");
        if (escritura && java.nio.file.Files.exists(ruta)
                && JOptionPane.showConfirmDialog(this, "El archivo ya existe. Desea reemplazarlo?",
                        "Guardar configuracion", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        procesarArchivo(ruta, escritura);
    }

    /** Recibe una ruta elegida; no espera por disco ni por el núcleo en EDT. */
    public void procesarArchivo(Path ruta, boolean escritura) {
        if (controlador == null || ruta == null) { resultado.setText("Archivo y controlador obligatorios"); return; }
        habilitar(false);
        resultado.setText(escritura ? "Guardando..." : "Cargando...");
        Thread trabajador = new Thread(() -> {
            try {
                if (escritura) {
                    ArchivoConfiguracion.guardar(ruta, controlador.getCargaInicial());
                    SwingUtilities.invokeLater(() -> resultado.setText("Guardado: " + ruta.getFileName()));
                } else {
                    CargaInicial carga = ArchivoConfiguracion.cargar(ruta);
                    controlador.cargarCarga(carga);
                    SwingUtilities.invokeLater(() -> { alCargar.accept(carga); resultado.setText("Cargado: " + ruta.getFileName()); });
                }
            } catch (java.io.IOException | RuntimeException error) {
                SwingUtilities.invokeLater(() -> resultado.setText("Error: " + error.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> { habilitar(true); });
            }
        }, "Archivo-configuracion");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    private void habilitar(boolean valor) {
        guardar.setEnabled(valor);
        cargar.setEnabled(valor);
        ejemplo.setEnabled(valor);
        abrirDisco.setEnabled(valor);
        abrirProcesos.setEnabled(valor);
        guardarDisco.setEnabled(valor);
        actualizarFaltante();
    }
    public String getResultado() { return resultado.getText(); }

    /** Carga los dos JSON del enunciado (disco y procesos) fuera del EDT. */
    public void cargarEjemplo() {
        enTrabajador("Cargando ejemplo...", () -> {
            controlador.cargarSistemaArchivos(Ejemplos.ArchivosEnunciado.SISTEMA_ARCHIVOS);
            controlador.cargarProcesos(Ejemplos.ArchivosEnunciado.PROCESOS);
            return "Ejemplo del enunciado listo, pulse Iniciar. " + describirDisco();
        });
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        guardar = new javax.swing.JButton(); cargar = new javax.swing.JButton(); ejemplo = new javax.swing.JButton(); resultado = new javax.swing.JLabel();
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        guardar.setText("Guardar CSV"); add(guardar);
        cargar.setText("Abrir CSV"); add(cargar);
        ejemplo.setText("Cargar ejemplo"); add(ejemplo);
        resultado.setText("Sin archivo"); add(resultado);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton guardar;
    private javax.swing.JButton cargar;
    private javax.swing.JButton ejemplo;
    private javax.swing.JLabel resultado;
    // End of variables declaration//GEN-END:variables
}
