package Interfaces;

import javax.swing.*;
import java.awt.*;

import Modelo.CargaInicial;
import Persistencia.ArchivoConfiguracion;
import Sistema.ControladorSimulacion;
import java.nio.file.Path;

/**
 * Selección de archivos en EDT y persistencia en un trabajador independiente.
 * @author Adolfo
 */
public class PanelArchivos extends JPanel {
    private final ControladorSimulacion controlador;
    private final java.util.function.Consumer<CargaInicial> alCargar;

    public PanelArchivos() { this(null, carga -> { }); }

    public PanelArchivos(ControladorSimulacion controlador, java.util.function.Consumer<CargaInicial> alCargar) {
        this.controlador = controlador;
        this.alCargar = alCargar;
        initComponents();
        guardar.addActionListener(evento -> seleccionar(true));
        cargar.addActionListener(evento -> seleccionar(false));
        ejemplo.addActionListener(evento -> cargarEjemplo());
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
    }
    public String getResultado() { return resultado.getText(); }

    /** Construye el ejemplo fuera del EDT y lo carga mediante el mismo controlador. */
    public void cargarEjemplo() {
        if (controlador == null) { resultado.setText("Sin controlador"); return; }
        habilitar(false);
        resultado.setText("Cargando ejemplo...");
        Thread trabajador = new Thread(() -> {
            try {
                CargaInicial carga = Ejemplos.ConfiguracionInicial.crear();
                controlador.cargarCarga(carga);
                SwingUtilities.invokeLater(() -> {
                    alCargar.accept(carga);
                    resultado.setText("Ejemplo listo · pulse Iniciar");
                });
            } catch (RuntimeException error) {
                SwingUtilities.invokeLater(() -> resultado.setText("Error: " + error.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    habilitar(true);
                });
            }
        }, "Cargar-ejemplo");
        trabajador.setDaemon(true); trabajador.start();
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
