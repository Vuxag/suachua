# Yogurt Machine Control App

Ứng dụng Android để giám sát và điều khiển máy làm sữa chua thông qua ESP32.

## Tính năng chính

- Hiển thị nhiệt độ hiện tại từ máy (qua ESP32)
- Cài đặt nhiệt độ mong muốn (ví dụ 42°C)
- Cài đặt thời gian lên men (ví dụ 8 tiếng)
- Gửi lệnh bắt đầu/dừng quá trình
- Hiển thị trạng thái hoạt động: Đang ủ, đã xong, nhiệt độ quá cao/thấp

## Công nghệ sử dụng

- Android Studio
- Kotlin
- MQTT Protocol
- Bluetooth Low Energy (BLE)
- ESP32 Microcontroller

## Cài đặt

1. Clone repository này
2. Mở project trong Android Studio
3. Build và chạy ứng dụng

## Cấu trúc project

- `app/src/main/java/com/example/yogurtmachine/` - Mã nguồn Kotlin
- `app/src/main/res/` - Tài nguyên (layout, drawable, values)
- `app/src/main/AndroidManifest.xml` - Cấu hình ứng dụng 