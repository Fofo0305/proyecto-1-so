package Interfaces;

import Modelo.VistaBuffer;
import Sistema.ControladorSimulacion;
import javax.swing.*;

/** Observa buffers sin esperar al nucleo en EDT. @author Luciano Rojas */
public final class PanelEstadoBuffers extends JPanel {
    private final ControladorSimulacion controlador;
    private final JTextArea contenido = new JTextArea(10, 40);
    private final java.util.concurrent.Semaphore lectura = new java.util.concurrent.Semaphore(1);
    private final Timer refresco;

    public PanelEstadoBuffers(ControladorSimulacion controlador) {
        this.controlador = controlador;
        setLayout(new java.awt.BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8,20,8,20));
        add(new JLabel("Estado de buffers y colas de semaforos"), java.awt.BorderLayout.NORTH);
        contenido.setEditable(false);
        contenido.setLineWrap(true);
        contenido.setWrapStyleWord(true);
        contenido.setText("Crea un buffer o carga una configuracion para consultar su estado.");
        add(new JScrollPane(contenido), java.awt.BorderLayout.CENTER);
        refresco = new Timer(200, evento -> actualizar());
    }

    @Override public void addNotify() { super.addNotify(); refresco.start(); actualizar(); }
    @Override public void removeNotify() { refresco.stop(); super.removeNotify(); }

    public void actualizar() {
        if (!lectura.tryAcquire()) return;
        Thread trabajador = new Thread(() -> {
            String texto;
            try {
                StringBuilder resumen = new StringBuilder();
                for (VistaBuffer buffer : controlador.consultarBuffers()) {
                    resumen.append(buffer.nombre()).append(" | computador ").append(buffer.anfitrion())
                            .append(" | elementos ").append(buffer.cantidad()).append('/').append(buffer.capacidad())
                            .append("\nContenido: ").append(buffer.contenido().isEmpty() ? "vacio" : buffer.contenido())
                            .append("\nmutex=").append(buffer.mutex()).append(" | vacios=").append(buffer.vacios())
                            .append(" | llenos=").append(buffer.llenos())
                            .append("\nEspera mutex: ").append(buffer.esperaMutex())
                            .append("\nEspera vacios: ").append(buffer.esperaVacios())
                            .append("\nEspera llenos: ").append(buffer.esperaLlenos()).append("\n\n");
                }
                texto = resumen.isEmpty() ? "Sin buffers registrados en esta sesion." : resumen.toString();
            } catch (RuntimeException error) {
                texto = "Error de lectura: " + error.getMessage();
            }
            final String resultado = texto;
            SwingUtilities.invokeLater(() -> {
                try {
                    if (!contenido.getText().equals(resultado)) {
                        int posicion = contenido.getCaretPosition();
                        contenido.setText(resultado);
                        contenido.setCaretPosition(Math.min(posicion, resultado.length()));
                    }
                } finally { lectura.release(); }
            });
        }, "Leer-buffers");
        trabajador.setDaemon(true);
        trabajador.start();
    }
}
