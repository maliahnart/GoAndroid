

  


🧠 Go Game (Cờ Vây) for Android
A strategic board game reimagined for Android with intelligent AI opponents


📱 Giới thiệu
Cờ Vây (Go) là một trò chơi chiến thuật cổ xưa bắt nguồn từ Trung Quốc, nổi tiếng với sự đơn giản trong luật chơi nhưng đòi hỏi tư duy chiến lược sâu sắc.Dự án này mang trò chơi Cờ Vây lên nền tảng Android với giao diện thân thiện, hỗ trợ nhiều chế độ chơi và các đối thủ AI thông minh.

🚀 Tính năng chính

🧩 Kích thước bảng cờ: 4x4,9x9, 13x13, 19x19 (tùy chọn).
🤖 Chơi với AI:  
Độ khó: Easy (Ngẫu nhiên), Medium (Kinh nghiệm),Hard (Alpha-Beta).  
AI thông minh với chiến lược từ cơ bản đến nâng cao.


👥 Chế độ hai người: Chơi đối kháng trên cùng thiết bị.
♻️ Tính năng gameplay: Undo (hoàn tác nước đi), Pass (bỏ lượt), Resign (đầu hàng).
📊 Lịch sử ván đấu: Ghi lại và xem lại các nước đi.(chưa làm).
☁️ Lưu trạng thái: Lưu và khôi phục ván cờ (đang phát triển).
⚡ Hiệu suất: Xử lý AI bất đồng bộ để đảm bảo giao diện mượt mà.


🛠️ Công nghệ sử dụng

🧱 Ngôn ngữ: Java  
📲 Nền tảng: Android Studio  
🧠 AI:  
Random AI (ngẫu nhiên).  
Heuristic-based AI (kinh nghiệm).  
Alpha-Beta Pruning (tìm kiếm cắt tỉa).  



🎨 Giao diện: XML Layouts + Custom Views (Canvas cho bảng cờ).  
📂 Mô hình: MVC (Model-View-Controller) với GameState, GameLogic, GameController.


🖼️ Ảnh minh họa

  
  
  


Ghi chú: Thay thế các ảnh placeholder trên bằng ảnh chụp màn hình thực tế của ứng dụng (bảng cờ, giao diện AI, menu).

📥 Cài đặt (dành cho nhà phát triển)
Yêu cầu:

Android Studio (phiên bản mới nhất, ví dụ: Koala | 2024.1.1).
JDK 17 hoặc cao hơn.
Emulator (API 33 khuyến nghị) hoặc thiết bị Android (minSdk 21).

Hướng dẫn:
# Sao chép mã nguồn
git clone https://github.com/yourusername/go-game-android.git
cd go-game-android

# Mở dự án trong Android Studio
# 1. Mở Android Studio.
# 2. Chọn File > Open, điều hướng đến thư mục go-game-android.

# Đồng bộ và xây dựng
# - Nhấn Sync Project with Gradle Files trong Android Studio.
# - Chọn Build > Rebuild Project.

# Chạy ứng dụng
# - Kết nối emulator (Pixel 6, API 33) hoặc thiết bị Android.
# - Nhấn Run > Run 'app'.

Lưu ý:

Nếu gặp lỗi I/O APK (Failed to open APK), thử:
Build > Clean Project, sau đó Rebuild Project.
Xóa ứng dụng khỏi emulator: adb uninstall com.example.gogameproject.
Tạo emulator mới hoặc kiểm tra dung lượng lưu trữ emulator.


Đảm bảo cấu hình Gradle đúng:android {
    compileSdk 33
    defaultConfig {
        applicationId "com.example.gogameproject"
        minSdk 21
        targetSdk 33
    }
}




🛠️ Cách chơi

Chọn chế độ:
PVB (Player vs Bot): Chọn độ khó (Beginner, Easy, Medium, Hard).
PVP (Player vs Player): Chơi hai người trên cùng thiết bị.


Đặt quân:
Chạm vào bảng cờ để đặt quân (Đen hoặc Trắng).
Sử dụng nút Pass, Resign, hoặc Undo trong giao diện.


AI thông minh:
AI Hard (MCTS) sử dụng 500 mô phỏng để chọn nước đi chiến lược.
Các chế độ khác nhanh hơn, phù hợp cho người mới.




📈 Kế hoạch phát triển

 Hỗ trợ lưu/và tải ván cờ qua bộ nhớ cục bộ.
 Thêm giao diện phân tích nước đi sau trận.
 Tối ưu MCTS cho bảng 19x19 (hiện tại tối ưu cho 9x9).
 Thêm hiệu ứng âm thanh và hình ảnh động.
 Hỗ trợ chế độ trực tuyến (multiplayer).


🤝 Đóng góp
Chúng tôi hoan nghênh mọi đóng góp! Để tham gia:

Fork dự án.
Tạo branch mới: git checkout -b feature/ten-tinh-nang.
Commit thay đổi: git commit -m "Thêm tính năng XYZ".
Push lên branch: git push origin feature/ten-tinh-nang.
Tạo Pull Request trên GitHub.


📬 Liên hệ

Email: secret
GitHub: maliahart
Issues: Báo lỗi hoặc đề xuất tại GitHub Issues


🎮 Hãy thưởng thức Cờ Vây trên Android và thử thách trí tuệ của bạn! 🧠
