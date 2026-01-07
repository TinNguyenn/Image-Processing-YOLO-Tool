import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {
        //Nạp thư viện
        String thuMucGoc = System.getProperty("user.dir");  //Lấy đường dẫn của project
        String duongDanThuVien = thuMucGoc + "\\lib\\opencv_java4120.dll";    //Lấy đường dẫn của thư viện
        System.load(duongDanThuVien);    //Nạp thư viện 

        //Cài giao diện FlatLaf
        UIManager.setLookAndFeel (new FlatLightLaf() );
        /*  User Interface Manager: quản lý giao diện của java swing
            setLookAndFeel: Thay đổi theme của giao diện
            Light: nền sáng
        */

        //Bo góc các nút 
        /*  .arc: Architecture Arc (độ cong của góc)
        */
        UIManager.put( "Button.arc", 10);       //Chỉnh cho nút bấm
        UIManager.put( "Component.arc", 10);    //Chỉnh cho các ô
        UIManager.put( "TextComponent.arc",10); //Chỉnh cho tất cả các thành phần khác

        //Bật giao diện 
        System.out.println("Dang mo giao dien...");
        new UI().setVisible(true);   //Mở giao diện lên
    }
}