import javax.swing.*;           //Các thành phần giao diện hiện đại của Java Swing
import javax.swing.border.*;    //Các kiểu border (viền) cho giao diện để trang trí
import javax.swing.event.DocumentEvent; // Thêm bắt sự kiện gõ phím
import javax.swing.event.DocumentListener;
import java.awt.*;              //Các thành phần đồ họa
import java.awt.event.*;        //Sự kiện chuột, bàn phím
import java.io.File;            //Làm việc với file và folder trên ổ cứng
import javax.imageio.ImageIO;   //Công cụ đọc/ghi file ảnh
import java.util.ArrayList;     //
import java.util.List;          //Công cụ quản lý danh sách động
import java.text.SimpleDateFormat; // Thêm thư viện để format ngày giờ
import java.util.Date;             // Thêm thư viện lấy ngày giờ

public class UI extends JFrame {
    
    //Các thành phần giao diện
    JLabel lblAnhGoc, lblAnhKetQua;    //Hiển thị ảnh
    JTextField txtRong, txtCao;        //Nhập kích thước khi resize
    
    // --- THÊM MỚI ---
    JComboBox<String> cboTyLe;   //Combobox chọn tỷ lệ
    JComboBox<String> cboModel;  //Combobox chọn Model AI
    // ----------------

    JCheckBox chkDenTrang, chkLatAnh, chkKhuNhieu;   //Nút chọn đen trắng, lật ảnh, khử nhiễu (Đã bỏ chkNhanDien)
    JButton btnChonInput, btnChonOutput, btnXuLyBatch, btnCatAnh;   //Nút chọn hành động
    JLabel lblPathOutput;              //Hiển thị đường dẫn để lưu ảnh kết quả

    //Biến lưu danh sách file và trạng thái
    JList<String> listFileNames;  //Biến hiển thị danh sách tên file (cái vỏ)
    DefaultListModel<String> listModel;  //Làm cầu nối trung gian
    List<File> danhSachFileGoc = new ArrayList<>();   //Biến lưu đường dẫn file gốc
    File fileAnhHienTai = null;   //Biến lưu file ảnh đang hiển thị
    File folderOutput = null;     //Biến lưu folder đầu ra

    //Biến crop ảnh
    int startX, startY, endX, endY;  //Tọa độ kéo chuột 
    int realH, realW;                //Kích thước thực của ảnh gốc
    int resultH, resultW;            //Kích thước thực của ảnh kết quả
    boolean dangKeoChuot = false;    //Biến lưu trạng thái kéo chuột 

    //Resize và hiển thị
    int hienThiW, hienThiH; 
    int offsetX = 0, offsetY = 0; 
    boolean isUpdating = false; // Cờ chặn vòng lặp vô tận khi resize

    //Các font chữ
    Font fontChuan = new Font("Segoe UI", Font.PLAIN, 13);    
    Font fontTieuDe = new Font("Segoe UI", Font.BOLD, 18);    
    Font fontCheckBox = new Font("Segoe UI", Font.PLAIN, 14); 
    Font fontNutChucNang = new Font("Segoe UI", Font.BOLD, 14);    
    Font fontTieuDeCaiDat = new Font("Segoe UI", Font.BOLD, 15);   
    Font fontLabelBold = new Font("Segoe UI", Font.BOLD, 14);       
    
    //Hàm khởi tạo giao diện
    public UI() {

        //Cài đặt cơ bản cho giao diện
        setTitle("Image Process");
        setSize(1150, 800); // Tăng kích thước chút cho thoáng
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));     

        //1. Giao diện bên trái
        JPanel pnlLeft = new JPanel();    
        pnlLeft.setPreferredSize(new Dimension(340, 0));    // Tăng chiều rộng lên 340 để chứa đủ checkbox 1 hàng
        pnlLeft.setBackground(new Color(245, 248, 250));           
        pnlLeft.setBorder(new EmptyBorder(15, 15, 15, 15));   
        pnlLeft.setLayout(new BorderLayout(0, 12));           

        //1.1. Phần trên cùng: Title + Nút chọn input
        JPanel pnlTop = new JPanel();     
        pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.Y_AXIS));     
        pnlTop.setBackground(null);             

        JLabel lblTitle = new JLabel("CÔNG CỤ TIỀN XỬ LÝ");    
        lblTitle.setFont(fontTieuDe);                            
        lblTitle.setForeground(new Color(0, 102, 204));   
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);      //Căn giữa

        JPanel pnlBtnInput = new JPanel(new GridLayout(1, 1));   
        pnlBtnInput.setOpaque(false);                              
        pnlBtnInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));   
        pnlBtnInput.setAlignmentX(Component.CENTER_ALIGNMENT);   // Căn giữa panel nút

        btnChonInput = taoNutThuong("1. Chọn Ảnh / Folder", new Color(255, 193, 7), Color.BLACK);  
        pnlBtnInput.add(btnChonInput);    

        pnlTop.add(lblTitle);      
        pnlTop.add(Box.createVerticalStrut(20));   
        pnlTop.add(pnlBtnInput);           

        //1.2. Phần giữa: Danh sách file ảnh
        listModel = new DefaultListModel<>();     
        listFileNames = new JList<>(listModel);   
        listFileNames.setFont(fontChuan);         
        listFileNames.setFixedCellHeight(26);      

        JScrollPane scrollList = new JScrollPane(listFileNames);     
        scrollList.setBorder(BorderFactory.createTitledBorder(null, "Danh sách ảnh", 
        TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, fontChuan));  
        scrollList.setBackground(Color.WHITE);    

        //1.3. Phần dưới cùng: Cài đặt
        JPanel pnlBot = new JPanel();    
        pnlBot.setLayout(new BoxLayout(pnlBot, BoxLayout.Y_AXIS));    
        pnlBot.setBackground(null);    

        //1.3.1: GridBagLayout cho Cài đặt (CĂN GIỮA)
        JPanel pnlSettings = new JPanel(new GridBagLayout());    
        pnlSettings.setOpaque(false);    
        pnlSettings.setAlignmentX(Component.CENTER_ALIGNMENT); //Căn giữa panel cài đặt
        GridBagConstraints gbc = new GridBagConstraints();       

        gbc.insets = new Insets(5, 0, 5, 5);  
        gbc.anchor = GridBagConstraints.CENTER; //Các thành phần con sẽ nằm giữa

        // Dòng 1: Tiêu đề Resize
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;     
        JLabel lblResize = new JLabel("Cài đặt Resize:");
        lblResize.setFont(fontLabelBold);
        lblResize.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa chữ
        pnlSettings.add(lblResize, gbc);    

        // Dòng 2: ComboBox Tỷ lệ
        gbc.gridy = 1; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        String[] tyLeOptions = {"Tự do", "Giữ tỷ lệ gốc", "16:9 (Youtube)", "4:3 (Camera)", "1:1 (Vuông)"};
        cboTyLe = new JComboBox<>(tyLeOptions);
        cboTyLe.setFont(fontChuan);
        ((JLabel)cboTyLe.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER); // Căn giữa chữ trong ComboBox
        pnlSettings.add(cboTyLe, gbc);

        // Dòng 3: Ô nhập liệu (Rộng x Cao)
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        JLabel lblW = new JLabel("Rộng:"); lblW.setFont(fontChuan);
        pnlSettings.add(lblW, gbc);

        gbc.gridx = 1; 
        txtRong = new JTextField("0");      
        txtRong.setFont(fontChuan);              
        txtRong.setHorizontalAlignment(JTextField.CENTER);    
        txtRong.setPreferredSize(new Dimension(60, 25));      
        pnlSettings.add(txtRong, gbc);          

        gbc.gridx = 2; 
        JLabel lblX = new JLabel(" x ", SwingConstants.CENTER); // Label "x"
        lblX.setFont(fontChuan);
        pnlSettings.add(lblX, gbc);      

        gbc.gridx = 3;      
        txtCao = new JTextField("0");       
        txtCao.setFont(fontChuan);                
        txtCao.setHorizontalAlignment(JTextField.CENTER);         
        txtCao.setPreferredSize(new Dimension(60, 25));       
        pnlSettings.add(txtCao, gbc);      

        // Khoảng cách
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        pnlSettings.add(Box.createVerticalStrut(10), gbc);

        // Dòng 4: Tiêu đề AI Model
        gbc.gridy = 4; 
        JLabel lblTitleAI = new JLabel("Chọn Model AI:"); // Dùng luôn JLabel thường
        lblTitleAI.setFont(fontLabelBold);
        lblTitleAI.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa chữ
        pnlSettings.add(lblTitleAI, gbc);

        // Dòng 5: ComboBox Model
        gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        String[] modelOptions = {"--- Không sử dụng ---", "YOLO (Xuất file CSV)"};
        cboModel = new JComboBox<>(modelOptions);
        cboModel.setFont(fontChuan);
        // Tô màu đỏ cho option YOLO để dễ nhìn
        cboModel.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value.toString().contains("YOLO")) c.setForeground(new Color(200, 0, 0));
                else c.setForeground(Color.BLACK);
                c.setHorizontalAlignment(SwingConstants.CENTER); 
                return c;
            }
        });
        pnlSettings.add(cboModel, gbc);
        
        pnlBot.add(pnlSettings);        
        pnlBot.add(Box.createVerticalStrut(10));    

        //1.3.2. Checkbox (Sửa lại thành 1 hàng ngang)
        JPanel pnlCheck = new JPanel(new GridLayout(1, 3, 5, 0));   // 1 dòng, 3 cột
        pnlCheck.setOpaque(false);               
        pnlCheck.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); 
        pnlCheck.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa panel

        chkDenTrang = new JCheckBox("Đen trắng");    
        chkDenTrang.setFont(fontCheckBox); chkDenTrang.setOpaque(false);       
        chkDenTrang.setHorizontalAlignment(SwingConstants.CENTER); // Căn chữ giữa nút

        chkLatAnh = new JCheckBox("Lật ảnh");     
        chkLatAnh.setFont(fontCheckBox); chkLatAnh.setOpaque(false);  
        chkLatAnh.setHorizontalAlignment(SwingConstants.CENTER);

        chkKhuNhieu = new JCheckBox("Khử nhiễu");  
        chkKhuNhieu.setFont(fontCheckBox); chkKhuNhieu.setOpaque(false);
        chkKhuNhieu.setHorizontalAlignment(SwingConstants.CENTER);

        pnlCheck.add(chkDenTrang);      
        pnlCheck.add(chkLatAnh); 
        pnlCheck.add(chkKhuNhieu);

        pnlBot.add(pnlCheck);      
        pnlBot.add(Box.createVerticalStrut(15));    

        //1.3. Nút chức năng
        JPanel pnlActions = new JPanel(new GridLayout(3, 1, 0, 10));    
        pnlActions.setOpaque(false);           
        pnlActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));   
        pnlActions.setAlignmentX(Component.CENTER_ALIGNMENT); //Căn giữa

        btnChonOutput = taoNutLon("2. Chọn Nơi Lưu", new Color(23, 162, 184), Color.WHITE);  
        btnXuLyBatch = taoNutLon("3. Chạy Tất Cả", new Color(40, 167, 69), Color.WHITE);
        btnCatAnh = taoNutLon("Cắt ảnh (Crop)", new Color(220, 53, 69), Color.WHITE);

        pnlActions.add(btnChonOutput);    
        pnlActions.add(btnXuLyBatch);
        pnlActions.add(btnCatAnh);

        pnlBot.add(pnlActions);         
        pnlBot.add(Box.createVerticalStrut(10));     

        lblPathOutput = new JLabel("Chưa chọn nơi lưu!");
        lblPathOutput.setFont(new Font("Segoe UI", Font.ITALIC, 12));  
        lblPathOutput.setForeground(Color.RED);          
        lblPathOutput.setAlignmentX(Component.CENTER_ALIGNMENT); //Căn giữa
        pnlBot.add(lblPathOutput);     

        pnlLeft.add(pnlTop, BorderLayout.NORTH); 
        pnlLeft.add(scrollList, BorderLayout.CENTER);
        pnlLeft.add(pnlBot, BorderLayout.SOUTH);

        //2. Giao diện bên phải
        JPanel pnlRight = new JPanel(new GridLayout(1, 2, 30, 0));   
        pnlRight.setBorder(new EmptyBorder(30, 30, 30, 30));       
        pnlRight.setBackground(Color.WHITE);        

        lblAnhGoc = new JLabel("Ảnh gốc", SwingConstants.CENTER) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (dangKeoChuot) {          
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.RED); g2.setStroke(new BasicStroke(2));
                    int x = Math.min(startX, endX); int y = Math.min(startY, endY);
                    int w = Math.abs(startX - endX); int h = Math.abs(startY - endY);
                    g2.drawRect(x, y, w, h);
                }
                if (fileAnhHienTai != null) veKichThuoc(g, realW, realH, getHeight());
            }
        };
        trangTriKhungAnh(lblAnhGoc);

        lblAnhKetQua = new JLabel("Kết quả", SwingConstants.CENTER) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (lblAnhKetQua.getIcon() != null) veKichThuoc(g, resultW, resultH, getHeight());
            }
        };
        trangTriKhungAnh(lblAnhKetQua);

        pnlRight.add(lblAnhGoc);     
        pnlRight.add(lblAnhKetQua);
        add(pnlLeft, BorderLayout.WEST);    
        add(pnlRight, BorderLayout.CENTER);  

        setupEvents();   
        setupResizeEvents(); // Thêm sự kiện resize thông minh
    }

    // Hàm xử lý resize
    private void setupResizeEvents() {
        cboTyLe.addActionListener(e -> updateResizeFromWidth());
        txtRong.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateResizeFromWidth(); }
            public void removeUpdate(DocumentEvent e) { updateResizeFromWidth(); }
            public void changedUpdate(DocumentEvent e) { updateResizeFromWidth(); }
        });
        txtCao.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateResizeFromHeight(); }
            public void removeUpdate(DocumentEvent e) { updateResizeFromHeight(); }
            public void changedUpdate(DocumentEvent e) { updateResizeFromHeight(); }
        });
    }

    private void updateResizeFromWidth() {
        if (isUpdating) return; 
        if (cboTyLe.getSelectedIndex() == 0) return; 
        try {
            String sVal = txtRong.getText().trim();
            if (sVal.isEmpty()) return;
            int w = Integer.parseInt(sVal);
            double ratio = getSelectedRatio();
            if (ratio > 0) {
                isUpdating = true; // Khóa
                int h = (int) (w / ratio);
                txtCao.setText(String.valueOf(h));
                isUpdating = false; // Mở
            }
        } catch (Exception ex) {}
    }

    private void updateResizeFromHeight() {
        if (isUpdating) return;
        if (cboTyLe.getSelectedIndex() == 0) return;
        try {
            String sVal = txtCao.getText().trim();
            if (sVal.isEmpty()) return;
            int h = Integer.parseInt(sVal);
            double ratio = getSelectedRatio();
            if (ratio > 0) {
                isUpdating = true;
                int w = (int) (h * ratio);
                txtRong.setText(String.valueOf(w));
                isUpdating = false;
            }
        } catch (Exception ex) {}
    }

    private double getSelectedRatio() {
        String selected = (String) cboTyLe.getSelectedItem();
        if (selected == null) return 0;
        if (selected.contains("Giữ tỷ lệ gốc")) {
            if (realH > 0) return (double) realW / realH;
            return 0;
        } else if (selected.contains("16:9")) return 16.0 / 9.0;
        else if (selected.contains("4:3")) return 4.0 / 3.0;
        else if (selected.contains("1:1")) return 1.0;
        return 0; 
    }

    //Hàm thiết lập sự kiện
    private void setupEvents() {

        lblAnhGoc.addMouseListener(new MouseAdapter() {    
            public void mousePressed(MouseEvent e) {       
                startX = e.getX(); startY = e.getY();      
                dangKeoChuot = true;                       
            }
            public void mouseReleased(MouseEvent e) {      
                endX = e.getX(); endY = e.getY();          
                lblAnhGoc.repaint();                       
            }
        });

        lblAnhGoc.addMouseMotionListener(new MouseMotionAdapter() {  
            public void mouseDragged(MouseEvent e) {   
                endX = e.getX(); endY = e.getY();      
                lblAnhGoc.repaint();                   
            }
        });

        btnChonInput.addActionListener(e -> {       
            JFileChooser fc = new JFileChooser(System.getProperty("user.dir")); 
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);   

            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {  
                File selected = fc.getSelectedFile();          
                listModel.clear(); danhSachFileGoc.clear();    

                File[] files;        
                if (selected.isDirectory()) {       
                    files = selected.listFiles();   
                } else {              
                    files = new File[] { selected };    
                }

                if (files != null) {    
                    for (File f : files) {      
                        if (isImageFile(f)) {   
                            danhSachFileGoc.add(f);    
                            listModel.addElement(f.getName());   
                        }
                    }
                }
                if (!listModel.isEmpty()) listFileNames.setSelectedIndex(0);  
            }
        });

        btnChonOutput.addActionListener(e -> {      
            JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));   
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);      
            fc.setDialogTitle("Chọn thư mục lưu ảnh/báo cáo");      

            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {   
                folderOutput = fc.getSelectedFile();            
                String path = folderOutput.getAbsolutePath();   
                
                String textHienThi;    
                if (path.length() > 30) {     
                    textHienThi = "..." + path.substring(path.length() - 30);  
                } else {                 
                    textHienThi = path;
                }
                lblPathOutput.setText("Lưu tại: " + textHienThi);         
                lblPathOutput.setForeground(new Color(0, 100, 0));   
            }
        });

        listFileNames.addListSelectionListener(e -> {     
            if (!e.getValueIsAdjusting() && listFileNames.getSelectedIndex() != -1) {  
                int index = listFileNames.getSelectedIndex();     
                fileAnhHienTai = danhSachFileGoc.get(index);      
                hienAnhGoc();                                     

                if (folderOutput == null) {                       
                    lblAnhKetQua.setIcon(null);             
                    lblAnhKetQua.setText("Chưa chọn nơi lưu");   
                    return;   
                }

                // Nếu đang chạy YOLO (Index 1) thì thông báo chế độ File
                if (cboModel.getSelectedIndex() == 1) {
                    lblAnhKetQua.setIcon(null);    
                    lblAnhKetQua.setText("Chế độ xuất file (Không hiện ảnh)");
                } else {
                    String tenFileKetQua = "Processed_" + fileAnhHienTai.getName();   
                    File fileKetQua = new File(folderOutput,tenFileKetQua);           

                    if (fileKetQua.exists()) {     
                        hienAnhLenLabel(fileKetQua, lblAnhKetQua, true);   
                    } else {                      
                        lblAnhKetQua.setIcon(null);    
                        lblAnhKetQua.setText("Chưa có kết quả. Cần nhấn xử lý!");   
                    }
                }
            }
        });

        //Nút xử lý tất cả 
        btnXuLyBatch.addActionListener(e -> {
            if (danhSachFileGoc.isEmpty() || folderOutput == null) {     
                JOptionPane.showMessageDialog(this, "Chưa chọn ảnh hoặc nơi lưu!");
                return;
            }

            // Xử lý logic tạo file CSV
            String csvPath = null;
            if (cboModel.getSelectedIndex() == 1) { 
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
                String timeStr = sdf.format(new Date());
                
                csvPath = folderOutput.getAbsolutePath() + "\\YOLO_Report_" + timeStr + ".csv";
                try (java.io.FileWriter w = new java.io.FileWriter(csvPath)) {
                    w.write("File_Name,Resolution,Time_ms,Detected_Objects\n");
                } catch (Exception ex) {}
                lblPathOutput.setText("Đang ghi: YOLO_Report_" + timeStr + ".csv");
            }

            for (File f : danhSachFileGoc) {   
                xuLyAnh(f, csvPath);      //Truyền thêm csvPath
            }
            
            int index = listFileNames.getSelectedIndex();  
            if (index != -1) {       
                listFileNames.clearSelection();    
                listFileNames.setSelectedIndex(index);    
            }
            JOptionPane.showMessageDialog(this, "Đã xử lý xong! Kiểm tra folder output.");
        });

        //Nút cắt ảnh
        btnCatAnh.addActionListener(e -> {     
            if (fileAnhHienTai == null) return;      
            
            // Tính toán tỷ lệ dựa trên kích thước ảnh đang hiển thị
            double tyLeX = (double) realW / hienThiW;    
            
            // Tính toạ độ chuột kéo trên UI
            int rectX = Math.min(startX, endX);
            int rectY = Math.min(startY, endY);
            int rectW = Math.abs(startX - endX);
            int rectH = Math.abs(startY - endY);

            int x = (int) ((rectX - offsetX) * tyLeX);          
            int y = (int) ((rectY - offsetY) * tyLeX);          
            int w = (int) (rectW * tyLeX);         
            int h = (int) (rectH * tyLeX);         

            String folder;
            if (folderOutput != null) {                      
                folder = folderOutput.getAbsolutePath();
            } else {                                         
                folder = System.getProperty("user.dir");
            }

            String kq = new ImageProcess().catAnh(fileAnhHienTai, folder, x, y, w, h);    
            if (kq != null) hienAnhLenLabel(new File(kq), lblAnhKetQua, true);  
        });
    }

    //Các hàm phụ
    boolean isImageFile(File f) {
        String n = f.getName().toLowerCase();  
        return n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".jpeg");  
    }

    //Hàm xử lý ảnh
    void xuLyAnh(File fileInput, String csvPath) {
        if (fileInput == null) return;       

        int rong = 0;
        int cao = 0;
        try {
            if (!txtRong.getText().trim().isEmpty()) rong = Integer.parseInt(txtRong.getText().trim());
            if (!txtCao.getText().trim().isEmpty()) cao = Integer.parseInt(txtCao.getText().trim());
        } catch (Exception e) {
            rong = 0; cao = 0;
        }

        String folder;                                   
        if (folderOutput != null) {                      
            folder = folderOutput.getAbsolutePath();
        } else {                                         
            folder = System.getProperty("user.dir");
        }
            
        ImageProcess mayXuLy = new ImageProcess();        
        boolean chayYolo = (cboModel.getSelectedIndex() == 1); // Kiểm tra xem có chọn YOLO ko

        mayXuLy.thucHienXuLy(fileInput, folder, rong, cao, 
            chkDenTrang.isSelected(), chkLatAnh.isSelected(), 
            chkKhuNhieu.isSelected(), chayYolo, csvPath);   //Truyền thêm tham số
    }

    //Hàm hiển thị ảnh gốc
    void hienAnhGoc() {
        if (fileAnhHienTai == null) return;    
        try {                        
            var img = ImageIO.read(fileAnhHienTai);  
            if (img == null) return;    
            realW = img.getWidth();     
            realH = img.getHeight();    
            hienAnhLenLabel(fileAnhHienTai, lblAnhGoc, false);  
            lblAnhGoc.repaint();        
        } catch (Exception e) {} 
    }
    
    //Hàm hiển thị ảnh lên Label 
    void hienAnhLenLabel(File f, JLabel lbl, boolean isKetQua) {
    try {
        if (lbl.getIcon() != null) {
            ((ImageIcon) lbl.getIcon()).getImage().flush(); 
        }
        lbl.setIcon(null);

        var img = ImageIO.read(f);    
        if (img == null) return;      

        // Lấy kích thước khung chứa
        int labelW = (lbl.getWidth() > 0) ? lbl.getWidth() : 300;
        int labelH = (lbl.getHeight() > 0) ? lbl.getHeight() : 300;

        // Lấy kích thước ảnh gốc
        int imgW = img.getWidth();
        int imgH = img.getHeight();

        // Tính tỷ lệ scale sao cho vừa khung mà không méo
        double scale = Math.min((double)labelW / imgW, (double)labelH / imgH);
        
        // Kích thước mới của ảnh hiển thị
        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);

        // Cập nhật các biến toàn cục để dùng cho chức năng cắt ảnh
        if (!isKetQua) { 
            hienThiW = newW;
            hienThiH = newH;
            offsetX = (labelW - newW) / 2;
            offsetY = (labelH - newH) / 2;
            realW = imgW; 
            realH = imgH;
        } else {
            resultW = imgW; 
            resultH = imgH;
        }

        lbl.setIcon(new ImageIcon(img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)));   
        lbl.setText("");        
        lbl.setHorizontalAlignment(JLabel.CENTER);
        lbl.setVerticalAlignment(JLabel.CENTER);

        if (isKetQua) { 
            resultW = img.getWidth(); resultH = img.getHeight(); 
        }
    } catch (Exception e) {}
    }

    //Hàm vẽ kích thước
    void veKichThuoc(Graphics g, int w, int h, int chieuCaoKhung) {
        String t = w + " x " + h + " px";
        g.setFont(fontChuan);
        g.setColor(Color.BLACK); 
        g.drawString(t, 10, chieuCaoKhung - 10);  
    }

    private JButton taoNutThuong(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(fontChuan);
        btn.setBackground(bg); btn.setForeground(fg);
        btn.setFocusPainted(false);                 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));   
        return btn;
    }

    private JButton taoNutLon(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(fontNutChucNang);
        btn.setBackground(bg); btn.setForeground(fg);
        btn.setFocusPainted(false);               
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));   
        return btn;
    }

    private void trangTriKhungAnh(JLabel lbl) {
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 14));  
        lbl.setForeground(Color.GRAY);     
        lbl.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 2)); 
    } 
}