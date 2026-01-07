# Công cụ tiền xử lý và nhận diện vật thể (Java + YOLOv3)

Một công cụ tiền xử lý ảnh và kết hợp với mô hình YOLOv3 để nhận diện vật thể 

---

## Tính năng chính
1. **Tiền xử lý ảnh (Preprocessing):**
   - Thay đổi kích thước (Resize) thông minh (giữ tỷ lệ).
   - Chuyển đổi ảnh sang Đen/Trắng (Grayscale).
   - Khử nhiễu (Denoising) sử dụng bộ lọc Gaussian Blur.
   - Lật ảnh (Flip), Cắt ảnh (Crop).

2. **Nhận diện vật thể (AI Object Detection):**
   - Tích hợp mô hình **YOLOv3** (You Only Look Once).
   - Nhận diện 80 loại vật thể phổ biến (COCO Dataset).
   - **Xuất báo cáo tự động:** Kết quả nhận diện được lưu vào file CSV (kèm thời gian thực thi) để phục vụ thống kê/nghiên cứu.

---

## Hướng dẫn cài đặt và chạy 

Do giới hạn dung lượng của GitHub, một số file thư viện nặng đã bị loại bỏ khỏi repository này. Để chạy được dự án, bạn cần thực hiện các bước sau:

### Bước 1: Chuẩn bị file thiếu
1. Vào thư mục `yolo/`:
   - Tải file **`yolov3.weights`** (khoảng 237MB) từ trang chủ YOLO hoặc Google Drive.
   - Đặt file này vào trong thư mục `yolo/` (ngang hàng với `yolov3.cfg`).

2. Vào thư mục `lib/`:
   - Đảm bảo đã có file **`opencv_java4120.dll`**. Nếu chưa có, vui lòng tải OpenCV 4.1.2 và copy file dll vào đây.

### Bước 2: Mở dự án với VS Code
1. Mở thư mục dự án bằng **VS Code**.
2. Đảm bảo đã cài đặt **Extension Pack for Java**.
3. Cấu trúc thư mục chuẩn sẽ trông như sau:

├── lib/
│   ├── flatlaf-3.2.5.jar
│   ├── opencv_java4120.dll
│   └── opencv_java4120.dll
├── src/
│   ├── ImageProcess.java
│   ├── Main.java
│   └── UI.java
├── yolo/
│   ├── coco.names
│   ├── yolov3.cfg
│   └── yolov3.weights
├── yolo_report/
├── .gitignore
└── README.md

### Bước 3: Chạy chương trình
- Mở file `src/Main.java`.
- Nhấn nút **Run** (hoặc `F5`).
- Giao diện chương trình sẽ hiện lên.

---

## Cấu trúc báo cáo CSV
Khi chạy chức năng **"3. Chạy Tất Cả"** với chế độ YOLO, file báo cáo sẽ được sinh ra trong thư mục `yolo_report/` với định dạng:

| File Name | Resolution | Time (ms) | Detected Objects |
|-----------|------------|-----------|------------------|
| Apple.jpg | 1920x1080 | 850 | apple |
| Dog.jpg | 500x500 | 420 | dog | bicycle |

