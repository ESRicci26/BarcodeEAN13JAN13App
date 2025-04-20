package javaricci.com.br;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.EAN13Writer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.awt.Dimension;


public class BarcodeEAN13JAN13App extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private JTextField dataField;
    private JLabel previewLabel;
    private JButton generateButton;
    private JButton saveButton;
    private JButton captureButton;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    
    private JPanel ean13Panel;
    private JTextField prefixField;
    private JTextField manufacturerField;
    private JTextField productField;
    private JTextField checkDigitField;
    private JButton calculateEan13Button;

    private BitMatrix bitMatrix;
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private boolean isCapturing = false;

    public BarcodeEAN13JAN13App() {
        super("Gerador de Códigos de Barras EAN-13/JAN-13");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 700);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

        JPanel generalDataPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        generalDataPanel.setBorder(new TitledBorder("Dados Gerais"));
        
        generalDataPanel.add(new JLabel("Dados:"));
        dataField = new JTextField(20);
        generalDataPanel.add(dataField);

        generalDataPanel.add(new JLabel("Largura:"));
        widthSpinner = new JSpinner(new SpinnerNumberModel(300, 100, 800, 10));
        generalDataPanel.add(widthSpinner);

        generalDataPanel.add(new JLabel("Altura:"));
        heightSpinner = new JSpinner(new SpinnerNumberModel(100, 50, 300, 10));
        generalDataPanel.add(heightSpinner);
        
        configPanel.add(generalDataPanel);
        
        ean13Panel = new JPanel();
        ean13Panel.setLayout(new BoxLayout(ean13Panel, BoxLayout.Y_AXIS));
        ean13Panel.setBorder(new TitledBorder("EAN-13 (Formato mais usado globalmente)"));
        
        JPanel ean13FieldsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        
        ean13FieldsPanel.add(new JLabel("Prefixo do País (3 dígitos):"));
        prefixField = createNumericTextField(3);
        prefixField.setText("789");
        ean13FieldsPanel.add(prefixField);
        
        ean13FieldsPanel.add(new JLabel("Código da Empresa (5 dígitos):"));
        manufacturerField = createNumericTextField(5);
        ean13FieldsPanel.add(manufacturerField);
        
        ean13FieldsPanel.add(new JLabel("Código do Produto (4 dígitos):"));
        productField = createNumericTextField(4);
        ean13FieldsPanel.add(productField);
        
        ean13FieldsPanel.add(new JLabel("Dígito Verificador:"));
        checkDigitField = new JTextField(1);
        checkDigitField.setEditable(false);
        checkDigitField.setBackground(new Color(240, 240, 240));
        ean13FieldsPanel.add(checkDigitField);
        
        ean13Panel.add(ean13FieldsPanel);
        
        JPanel ean13ButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        calculateEan13Button = new JButton("Calcular EAN-13 e Gerar");
        calculateEan13Button.addActionListener(e -> generateEAN13());
        ean13ButtonPanel.add(calculateEan13Button);
        ean13Panel.add(ean13ButtonPanel);
        
        configPanel.add(ean13Panel);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        
        generateButton = new JButton("Gerar Código de Barras");
        generateButton.addActionListener(e -> generateEAN13());
        buttonPanel.add(generateButton);

        saveButton = new JButton("Salvar Imagem");
        saveButton.addActionListener(e -> saveBarcode());
        saveButton.setEnabled(false);
        buttonPanel.add(saveButton);
        
        captureButton = new JButton("Capturar com Câmera");
        captureButton.addActionListener(e -> toggleCameraCapture());
        buttonPanel.add(captureButton);
        
        configPanel.add(buttonPanel);
        
        mainPanel.add(configPanel, BorderLayout.NORTH);

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Visualização"));
        previewLabel = new JLabel("Aqui será exibida a visualização do código de barras", JLabel.CENTER);
        previewPanel.add(new JScrollPane(previewLabel), BorderLayout.CENTER);
        mainPanel.add(previewPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        JTextArea infoText = new JTextArea(
                "Informações EAN-13:\n" +
                "- Prefixos brasileiros: 789 e 790\n" + 
                "- O código deve ter exatamente 13 dígitos\n" +
                "- O dígito verificador é calculado automaticamente\n" +
                "- Exemplo: 789 (Prefixo) + 10003 (Empresa) + 1550 (Produto) + 7 (Dígito)"
        );
        infoText.setEditable(false);
        infoText.setBackground(new Color(240, 240, 240));
        infoText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(infoText);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }


    private void toggleCameraCapture() {
        if (!isCapturing) {
            startCameraCapture();
        } else {
            stopCameraCapture();
        }
    }


    private void startCameraCapture() {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                JOptionPane.showMessageDialog(this, 
                    "Nenhuma câmera encontrada.", 
                    "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            webcamPanel = new WebcamPanel(webcam);
            webcamPanel.setPreferredSize(new Dimension(640, 480));
            webcam.setViewSize(webcam.getViewSizes()[webcam.getViewSizes().length-1]);//RICCI
            
            JFrame cameraFrame = new JFrame("Captura de Código de Barras");
            cameraFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            cameraFrame.add(webcamPanel);
            cameraFrame.pack();
            cameraFrame.setLocationRelativeTo(this);
            cameraFrame.setVisible(true);
            
            isCapturing = true;
            captureButton.setText("Parar Captura");
            
            new Thread(() -> {
                while (isCapturing && webcam != null && webcam.isOpen()) {
                    try {
                        BufferedImage image = webcam.getImage();
                        if (image != null) {
                            String barcode = scanBarcode(image);
                            // Validação adicionada aqui:
                            if (barcode != null && isValidEAN13(barcode)) { // Só aceita se for EAN-13 válido
                                SwingUtilities.invokeLater(() -> {
                                    dataField.setText(barcode);
                                    if (barcode.length() == 13) {
                                        prefixField.setText(barcode.substring(0, 3));
                                        manufacturerField.setText(barcode.substring(3, 8));
                                        productField.setText(barcode.substring(8, 12));
                                        checkDigitField.setText(barcode.substring(12));
                                    }
                                    cameraFrame.dispose();
                                    stopCameraCapture();
                                    JOptionPane.showMessageDialog(null, "Código válido: " + barcode, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                                });
                                break;
                            }
                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            
            cameraFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    stopCameraCapture();
                }
            });
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao iniciar câmera: " + e.getMessage(), 
                "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }


    private void stopCameraCapture() {
        isCapturing = false;
        captureButton.setText("Capturar com Câmera");
        if (webcam != null) {
            webcam.close();
        }
    }


    //MÉTODO USANDO A CÂMERA
    private String scanBarcode(BufferedImage image) {
        try {
            // Aplicar pré-processamento na imagem
            BufferedImage processedImage = enhanceBarcodeImage(image);
            
            LuminanceSource source = new BufferedImageLuminanceSource(processedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.EAN_13));
            hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE); // Focar apenas em códigos bem formados
            
            Result result = new MultiFormatReader().decode(bitmap, hints);
            if (result != null && result.getBarcodeFormat() == BarcodeFormat.EAN_13) {
                String code = result.getText();
                if (isValidEAN13(code)) { // Verificar se o código é válido
                    return code;
                }
            }
        } catch (NotFoundException e) {
            // Código não encontrado
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private BufferedImage enhanceBarcodeImage(BufferedImage original) {
        // Converter para escala de cinza
        BufferedImage gray = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(original, 0, 0, null);
        
        // Aumentar contraste
        RescaleOp rescaleOp = new RescaleOp(1.5f, -30f, null);
        rescaleOp.filter(gray, gray);
        
        return gray;
    }


    private boolean isValidEAN13(String ean13) {
        if (ean13 == null || ean13.length() != 13 || !ean13.matches("\\d{13}")) {
            return false;
        }
        
        // Calcular dígito verificador
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(ean13.charAt(i));
            sum += (i % 2 == 0) ? digit * 1 : digit * 3;
        }
        int checksum = (10 - (sum % 10)) % 10;
        int lastDigit = Character.getNumericValue(ean13.charAt(12));
        
        return checksum == lastDigit;
    }


    private JTextField createNumericTextField(final int maxLength) {
        JTextField textField = new JTextField(maxLength);
        PlainDocument doc = (PlainDocument) textField.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String newText = fb.getDocument().getText(0, fb.getDocument().getLength()) + text;
                if ((fb.getDocument().getLength() + text.length() - length) <= maxLength && text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        return textField;
    }


    private void generateEAN13() {
        String prefix = prefixField.getText();
        String manufacturer = manufacturerField.getText();
        String product = productField.getText();
        
        if (prefix.length() != 3 || manufacturer.length() != 5 || product.length() != 4) {
            JOptionPane.showMessageDialog(this, 
                    "Por favor, preencha todos os campos com o número correto de dígitos:\n" +
                    "- Prefixo: 3 dígitos\n" +
                    "- Código da Empresa: 5 dígitos\n" +
                    "- Código do Produto: 4 dígitos", 
                    "Dados Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ean12 = prefix + manufacturer + product;
        int checkDigit = calculateEAN13CheckDigit(ean12);
        checkDigitField.setText(String.valueOf(checkDigit));
        String ean13 = ean12 + checkDigit;
        dataField.setText(ean13);
        
        try {
            int width = (int) widthSpinner.getValue();
            int height = (int) heightSpinner.getValue();
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            
            bitMatrix = new EAN13Writer().encode(ean13, BarcodeFormat.EAN_13, width, height, hints);
            
            updatePreview();
            saveButton.setEnabled(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar código de barras EAN-13: " + e.getMessage(), 
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }


    private int calculateEAN13CheckDigit(String ean12) {
        if (ean12 == null || ean12.length() != 12 || !ean12.matches("\\d{12}")) {
            throw new IllegalArgumentException("O código EAN-12 deve ter exatamente 12 dígitos numéricos");
        }
        
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(ean12.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        
        return (10 - (sum % 10)) % 10;
    }


    private void updatePreview() {
        try {
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ImageIcon icon = new ImageIcon(image);
            previewLabel.setIcon(icon);
            previewLabel.setText(null);
            
        } catch (Exception e) {
            previewLabel.setIcon(null);
            previewLabel.setText("Erro ao exibir visualização");
            e.printStackTrace();
        }
    }


    private void saveBarcode() {
        if (bitMatrix == null) {
            JOptionPane.showMessageDialog(this, "Gere um código de barras primeiro", 
                    "Nenhum código gerado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Código de Barras");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagens PNG", "png"));
        fileChooser.setSelectedFile(new File("barcode.png"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }

            try {
                Path path = FileSystems.getDefault().getPath(fileToSave.getAbsolutePath());
                MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
                
                JOptionPane.showMessageDialog(this, "Código de barras salvo com sucesso em:\n" + 
                        fileToSave.getAbsolutePath(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + e.getMessage(), 
                        "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
        	BarcodeEAN13JAN13App app = new BarcodeEAN13JAN13App();
            app.setVisible(true);
        });
    }
}
