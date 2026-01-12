import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;   //Dùng để đọc file ảnh từ ổ cứng và lưu file ảnh xuống ổ cứng
import org.opencv.imgproc.Imgproc;       //Dùng để gọi các hàm như resize(), cvtColor(), GaussianBlur()

import java.io.File;                     //Dùng để lấy đườn dẫn file
import java.io.FileWriter;               //Dùng để ghi ra file csv

import java.util.ArrayList;              //Tạo danh sách mảng động để
import java.util.List;                   //lưu các tên vật thể được nhận diện

import java.util.Scanner;                //Dùng để đọc file coco.names từng dòng và nạp vào chương trình
import org.opencv.dnn.*;                 //Dùng để chạy YOLO

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

        //Chọn YOLO và xuất file csv
        if (batYolo && csvPath != null) {    
            if (kq.channels() == 1) Imgproc.cvtColor(kq, kq, Imgproc.COLOR_GRAY2BGR);  
            /*   // Nếu ảnh đang đen trắng thì chuyển lại màu để model chạy chuẩn
                 Đen trắng (Grayscale): có 1 kênh
                 Ảnh màu (BGR - Blue, Green, Red): có 3 kênh
            */

            String folderProject = System.getProperty("user.dir");   //Lấy đường dẫn project
            // Gọi hàm nhận diện và ghi file
            chayYoloVaGhiFile(kq, folderProject, fileVao.getName(), csvPath);   //Gọi hàm bên dưới
            
            src.release(); kq.release();     //Giải phóng bộ nhớ
            return null; // Không trả về đường dẫn ảnh vì không lưu ảnh
        }

        //Nếu không chọn YOLO thì lưu ảnh kết quả bình thường
        new File(folderRa).mkdirs();   //Nếu chưa có thư mục ra thì tự tạo để tránh lôix
        String path = folderRa + "\\Processed_" + fileVao.getName(); //Tạo tên file mới
        Imgcodecs.imwrite(path, kq);    
        /*  Lệnh ghi file, lấy dữ liệu từ biến kq, nén thành file ảnh 
            và lưu xuống đường dẫn path 
        */  

        src.release();  kq.release();   //Giải phóng bộ nhớ
        return path; // Trả về đường dẫn để UI hiện lên
    }

    // Hàm chạy YOLO và ghi vào file csv
    private void chayYoloVaGhiFile(Mat src, String folderProject, String fileName, String csvPath) {
        try {
            // Load Model
            String w = folderProject + "\\yolo\\yolov3.weights";
            String c = folderProject + "\\yolo\\yolov3.cfg";
            String n = folderProject + "\\yolo\\coco.names";

            Net net = Dnn.readNetFromDarknet(c, w);  
            /*  YOLO gốc được viết trên một framework là Darknet (viết bằng C)
                OpenCV cần dùng hàm chuyên biệt này để đọc hiểu được định dạng file của Darknet
            */
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV); //Dùng chính bộ tính toán C++ mặc định của OpenCV để chạy mạng này
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU);    //Chạy bằng CPU

            long start = System.currentTimeMillis();  //Thời gian hiện tại trước khi nhận diện, để tí tính thời gian nhận diện

            // Nhận diện vật thể 
            Mat blob = Dnn.blobFromImage(src, 1.0/255, new Size(416, 416), new Scalar(0,0,0), true, false);
            /*  Mat blob: Đây không phải là ảnh thường nữa. Blob (Binary Large Object) là một khối dữ liệu 4 chiều 
            (Số lượng ảnh, Số kênh màu, Chiều cao, Chiều rộng) mà mạng nơ-ron có thể hiểu được.
                src: file ảnh đầu vào
                1.0/255: ảnh gốc có giá trị pixel từ 0-255 (quá lớn để tính toán nhanh). Nhân với 1/255 để ép về 0-1 giúp AI tính toán
                new Size(416,416): Yolov3 được huấn luyện với kích thước ảnh vuông 416x416 nên ta ép ảnh về đúng kích thước
                new Scalar(0,0,0): Mean subtraction (Trừ giá trị trung bình). Ở đây ta để 0, tức là không trừ gì cả
                true (SwapRB): OpenCV đọc ảnh theo chuẩn BGR, còn YOLO được huấn luyện theo chuẩn RGB, nên ta đổi lại 
                false (crop): ko crop chỉ resize
            */

            net.setInput(blob);  //Đưa dữ liệu vào mạng 

            List<Mat> outs = new ArrayList<>();  
            /*  YOLOv3 là một mạng phức tạp, nó không chỉ trả về 1 kết quả
                Nó có 3 lớp đầu ra (Output Layers) tương ứng với việc nhận diện vật thể ở 3 kích thước khác nhau (lớn, trung bình, nhỏ)
                Do đó, ta cần một cái danh sách (List<Mat>) để hứng trọn bộ 3 kết quả này
             */

            net.forward(outs, net.getUnconnectedOutLayersNames());
            /*  net.forward(...): Đây là lệnh tốn CPU nhất. Nó ra lệnh cho dữ liệu chạy từ đầu vào, qua hàng trăm lớp ẩn, 
            nhân ma trận liên tục để ra đến đầu ra. 
                outs: Kết quả sau khi chạy xong sẽ đổ vào biến này
                net.getUnconnectedOutLayersNames(): Hàm này tự động tìm tên của các lớp cuối cùng trong mạng YOLO 
            (thường tên là yolo_82, yolo_94, yolo_106) để trích xuất dữ liệu tại đó 
            */

            // YOLO chỉ trả về những con số, ví dụ là thấy vật thể số 0. Đoạn code này dùng để chuyển từ số qua chữ (vd 0 -> bus)
            List<String> names = new ArrayList<>();    //Tạo 1 list rỗng để chuẩn bị chứa tên của 80 vật thể để truy xuất dữ liệu sau

            try (Scanner sc = new Scanner(new File(n))) {
                while (sc.hasNextLine()) names.add(sc.nextLine());    //Vòng lặp nạp dữ liệu
            }
            /*  n: Đường dẫn tới coco.names
                Scanner: Công cụ giúp đọc file văn bản từng dòng một 
                Bình thường mở file xong phải nhớ lệnh close() để đóng lại (không là bị rò rỉ bộ nhớ)
            Viết trong ngoặc try (...) thế này thì Java sẽ tự động đóng file ngay khi đọc xong
            */

            // Lọc kết quả
            List<String> detectedObjects = new ArrayList<>();    //List lưu kết quả sau khi lọc cuối cùng (vd apple)
            float confThreshold = 0.5f;     //Confidence threshold (ngưỡng tin cậy): đặt là 50%, trên 50% thì mới xét

            List<Rect2d> boxes = new ArrayList<>();     //Lưu thông tin các hộp sau khi chạy YOLO
            List<Float> confs = new ArrayList<>();      //Confidence Score: điểm đánh giá khi nhận diện (ví dụ 0.1 = 10%)
            List<Integer> classIds = new ArrayList<>(); //Để chứa số nguyên, vì YOLO nhận diện là ra số chứ ko ra chữ (số thự tự trong coco.names)

            for (Mat out : outs) {            //Duyệt qua 3 lớp đầu ra của YOLOv3 (13x13, 26x26, 52x52)
                for (int i = 0; i < out.rows(); i++) {       //DUyệt từng dòng trong mỗi lớp đấy, mỗi dòng là 1 hộp
                    Mat row = out.row(i);             //Lấy dữ liệu dòng hiện tại
                    Mat scores = row.colRange(5, out.cols());      //Cắt lấy phần điểm số, từ cột 5 trở đi
                    Core.MinMaxLocResult result = Core.minMaxLoc(scores);    //Tìm giá trị lớn nhất và điểm số của chúng (ko quan tâm giá trị min)
                    float conf = (float) result.maxVal;      //Lấy ra giá trị lớn nhất và lưu vào biến conf

                    if (conf > confThreshold) {     //Nếu conf > 0.5
                        int centerX = (int) (row.get(0, 0)[0] * src.cols());    //Tâm vật thể nằm ở pixel bao nhiêu, đổi từ 0-1 sang pixel 
                        int centerY = (int) (row.get(0, 1)[0] * src.rows());    //Tâm vật thể nằm ở pixel bao nhiêu, đổi từ 0-1 sang pixel 
                        int width = (int) (row.get(0, 2)[0] * src.cols());      //Vật thể rộng bao nhiêu pixel
                        int height = (int) (row.get(0, 3)[0] * src.rows());     //Vật thể cao bao nhiêu pixel
                        int left = centerX - width / 2;       //Tọa độ X của mép trái
                        int top = centerY - height / 2;       //Tọa độ Y của mép trên

                        boxes.add(new Rect2d(left, top, width, height));    //Lưu vào list boxes về tọa độ
                        classIds.add((int) result.maxLoc.x);                //Lưu vị trí của xác suất cao nhất vào list classIds
                        confs.add(conf);            //Lưu giá trị conf vào list confs
                    }
                }
            }

            // NMS để lọc trùng  (Non-maximum Suppression)
            if (!boxes.isEmpty()) {    
                /*Các biến boxes và confs đang ở java list, còn openCV được viết bằng C++ nên yêu cầu đầu vào là Ma trận, các bước sau 
                chuyển nó về ma trận cột N x 1 */
                MatOfRect2d boxMat = new MatOfRect2d(); boxMat.fromList(boxes);
                MatOfFloat confMat = new MatOfFloat(); confMat.fromList(confs);
                MatOfInt indices = new MatOfInt();      //Rỗng để chứa kết quả đầu ra

                Dnn.NMSBoxes(boxMat, confMat, 0.5f, 0.4f, indices);
                /*  boxMat: danh sách vị trí các hộp (kể cả hộp trùng, hộp lệch)
                    confMat: bảng điểm tương ứng của các hộp
                    score_threshold: 0.5f: Bất kì hộp nào có độ tin cậy < 50% thì loại luôn (dù ở trên đã lọc rồi nhưng mà ở đây nhập lại vì tham số bắt buộc)
                    nms_threshold: 0.4f (IoU - Intersection over Union): Quy định thế nào là trùng nhau
                        lấy 2 cái hộ chồng lên nhau. Tính diện tích phần giao nhau (intersection) chia cho tổng diện tích bao phủ (union)
                        Nếu số đấy > 40% thì là trùng nhau, xóa thằng có điểm tin cậy thấp hơn
                    indices: kết quả lưu vào đây, trả về danh sách số thứ tự của của những hộp được giữ lại. Ví dụ indices = [0, 5, 12]
                */

                for (int i : indices.toArray()) {    //Duyệt từng thằng trong indices sau khi biến nó thành mảng
                    int id = classIds.get(i);        //Lấy trong classIds ra xem nó là số gì, ví dụ là 5 thì tìm đến dòng thứ 5 xem là con gì (0: con chó,...)
                    if (id < names.size()) {         //Check xem có vượt quá 80 ko ,nếu có thì lỗi
                        detectedObjects.add(names.get(id)); // Lấy tên vật thể và lưu vào list detectedObjects
                    }
                }
            }

            long time = System.currentTimeMillis() - start;         //Tính thời gian nhận diện để tí ghi ra file csv

            //Ghi vào CSV
            //Format: Tên_File, Kích_Thước, Thời_Gian, Danh_Sách_Vật_Thể
            String listObjStr = String.join(" | ", detectedObjects); //Ví dụ: person | car | dog
            if (listObjStr.isEmpty()) listObjStr = "None";      //Trường hợp ko thấy gì cho là None

            FileWriter writer = new FileWriter(csvPath, true);   //Mở file ở chế độ nối tiếp, nếu ko thì khi mở ra mất hết những gì đã lưu 
            String line = String.format("%s,%dx%d,%d,%s\n", fileName, src.cols(), src.rows(), time, listObjStr);  //Gộp thành 1 string
            writer.append(line);    //Ghi vào file csv
            writer.close();         //Cần có lệnh này để lưu lại những gì đã ghi

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