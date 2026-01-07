import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.opencv.dnn.*;   //Thư viện deep learning để dùng YOLO

public class ImageProcess {

    // Hàm xử lý nhận đủ tùy chọn từ UI
    public String thucHienXuLy(File fileVao, String folderRa, int rong, int cao, boolean denTrang,
    boolean latAnh, boolean khuNhieu, boolean batYolo, String csvPath) {
        
        Mat src = Imgcodecs.imread(fileVao.getAbsolutePath()); 
        /*  Imgcodecs.imread(): Đọc file từ ổ cứng
            fileVao.getAbsolutePath(): lấy đường dẫn đầy đủ của file ảnh
        */ 
                                                               
        //Resize 
        Mat kq = new Mat();
        if (rong > 0 && cao > 0) {    //Chỉ resize nếu người dùng nhập rộng >0 và cao >0
            Imgproc.resize(src, kq, new Size(rong, cao));
        } else {
            src.copyTo(kq);
        }

        //Khử nhiễu
        if (khuNhieu) {
            Imgproc.GaussianBlur (kq, kq, new Size (5,5), 0);
        }
        /*  GaussianBlur: Làm mịn ảnh để loại bỏ hạt nhiễu 
            Size(5,5): Kích thước hạt nhân làm mờ
            Số càng to (7,7), (9,9) thì càng mịn nhưng càng mờ. Thường là dùng (5,5)
         */

        //Đen trắng
        if (denTrang) Imgproc.cvtColor(kq, kq, Imgproc.COLOR_BGR2GRAY);
        /*  if (denTrang): Kiểm tra xem bên giao diện người dùng có tích ô "Đen trắng" không
            Imgproc.cvtColor(): Hàm chuyển đổi hệ màu
            COLOR_BGR2GRAY: Chuyển từ màu (Blue-Green-Red) sang xám (Gray)
        */

        //Lật ảnh ngang
        if (latAnh) Core.flip(kq, kq, 1);
        /*  if (latAnh): Kiểm tra xem người dùng có tích ô "Lật ảnh" không
            Core.flip(): Hàm lật ảnh của OpenCV
            Tham số 1: lật theo trục dọc (0 thì là trục ngang)
        */

        // 3. LOGIC YOLO MỚI: Chỉ xuất báo cáo, KHÔNG vẽ lên ảnh, KHÔNG lưu ảnh
        if (batYolo && csvPath != null) {
            // Nếu ảnh đang đen trắng thì chuyển lại màu để model chạy chuẩn
            if (kq.channels() == 1) Imgproc.cvtColor(kq, kq, Imgproc.COLOR_GRAY2BGR);
            
            String projectDir = System.getProperty("user.dir");
            // Gọi hàm nhận diện và ghi file
            chayYoloVaGhiFile(kq, projectDir, fileVao.getName(), csvPath);
            
            src.release(); kq.release();
            return null; // Không trả về đường dẫn ảnh vì không lưu ảnh
        }

        // 4. Nếu KHÔNG chọn YOLO thì lưu ảnh bình thường
        new File(folderRa).mkdirs(); 
        String path = folderRa + "\\Processed_" + fileVao.getName(); //Tạo tên file mới
        Imgcodecs.imwrite(path, kq);    
        /*  Lệnh ghi file, lấy dữ liệu từ biến kq, nén thành file ảnh 
            và lưu xuống đường dẫn path 
        */  

        //Giải phóng bộ nhớ
        src.release();
        kq.release();
        
        return path; // Trả về đường dẫn để UI hiện lên
    }

    // --- HÀM CHẠY YOLO VÀ GHI TÊN VẬT THỂ ---
    private void chayYoloVaGhiFile(Mat frame, String folderProject, String fileName, String csvPath) {
        try {
            // Load Model
            String w = folderProject + "\\yolo\\yolov3.weights";
            String c = folderProject + "\\yolo\\yolov3.cfg";
            String n = folderProject + "\\yolo\\coco.names";

            Net net = Dnn.readNetFromDarknet(c, w);
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU);

            long start = System.currentTimeMillis();

            // Detect
            Mat blob = Dnn.blobFromImage(frame, 1.0/255, new Size(416, 416), new Scalar(0,0,0), true, false);
            net.setInput(blob);
            List<Mat> outs = new ArrayList<>();
            net.forward(outs, net.getUnconnectedOutLayersNames());

            // Đọc tên class
            List<String> names = new ArrayList<>();
            try (Scanner sc = new Scanner(new File(n))) {
                while (sc.hasNextLine()) names.add(sc.nextLine());
            }

            // Lọc kết quả
            List<String> detectedObjects = new ArrayList<>();
            float confThreshold = 0.5f; 
            List<Rect2d> boxes = new ArrayList<>();
            List<Float> confs = new ArrayList<>();
            List<Integer> classIds = new ArrayList<>();

            for (Mat out : outs) {
                for (int i = 0; i < out.rows(); i++) {
                    Mat row = out.row(i);
                    Mat scores = row.colRange(5, out.cols());
                    Core.MinMaxLocResult result = Core.minMaxLoc(scores);
                    float conf = (float) result.maxVal;

                    if (conf > confThreshold) {
                        int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                        int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                        int width = (int) (row.get(0, 2)[0] * frame.cols());
                        int height = (int) (row.get(0, 3)[0] * frame.rows());
                        int left = centerX - width / 2;
                        int top = centerY - height / 2;

                        boxes.add(new Rect2d(left, top, width, height));
                        classIds.add((int) result.maxLoc.x);
                        confs.add(conf);
                    }
                }
            }

            // NMS để lọc trùng
            if (!boxes.isEmpty()) {
                MatOfRect2d boxMat = new MatOfRect2d(); boxMat.fromList(boxes);
                MatOfFloat confMat = new MatOfFloat(); confMat.fromList(confs);
                MatOfInt indices = new MatOfInt();
                Dnn.NMSBoxes(boxMat, confMat, 0.5f, 0.4f, indices);

                for (int i : indices.toArray()) {
                    int id = classIds.get(i);
                    if (id < names.size()) {
                        detectedObjects.add(names.get(id)); // Lấy tên vật thể
                    }
                }
            }

            long time = System.currentTimeMillis() - start;

            // Ghi vào CSV
            // Format: Tên_File, Kích_Thước, Thời_Gian, Danh_Sách_Vật_Thể
            String listObjStr = String.join(" | ", detectedObjects); // Ví dụ: person | car | dog
            if (listObjStr.isEmpty()) listObjStr = "None";

            FileWriter writer = new FileWriter(csvPath, true); // true = append
            String line = String.format("%s,%dx%d,%d,%s\n", 
                fileName, frame.cols(), frame.rows(), time, listObjStr);
            writer.append(line);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Hàm crop ảnh
    public String catAnh(File fileVao, String folderRa, int x, int y, int w, int h) {
        /* x,y là tọa độ của điểm đặt đầu tiên (góc trái trên cùng của vùng cần cắt)
           Hệ tọa độ ảnh trong máy tính, (0,0) là điểm góc trên cùng bên trái của ảnh
           Trục x chạy từ trái qua phải
           Trục y chạy từ trên xuống dưới
           w (width): Chiều rộng vùng muốn cắt
           h (height): Chiều cao vùng muốn cắt
        */ 

        //Đọc ảnh gốc
        Mat src = Imgcodecs.imread(fileVao.getAbsolutePath());

        //Kiểm tra tọa độ 
        //TH1: Cắt ra ngoài mép trái và mép trên thì ép về (0,0)
        if (x < 0) x = 0;      
        if (y < 0) y = 0;
        //TH2: Cắt ra ngoài mép phải và mép dưới thì thu gọn lại vừa kích thước ảnh
        if (x + w > src.width()) w = src.width() - x;
        if (y + h > src.height()) h = src.height() - y;

        //Tạo vùng cắt (Region of Interest - ROI)
        Rect vungCat = new Rect(x, y, w, h);  //Tạo một cái khung hình chữ nhật có tọa độ là vùng cần cắt
        Mat anhDaCat = new Mat(src, vungCat); // Cắt lấy vùng này

        //Lưu ảnh đã cắt
        String path = folderRa + "\\Cropped_" + fileVao.getName();
        Imgcodecs.imwrite(path, anhDaCat);

        //Giải phóng bộ nhớ
        src.release();
        anhDaCat.release();
        return path;
    }
}