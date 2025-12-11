# API Test Chat History - Ví dụ Response

## Endpoint
```
GET /test/chat-history?conversationId={uuid}&userId={uuid}&excludeMessageId={uuid}
```

## Ví dụ Request
```
GET /test/chat-history?conversationId=123e4567-e89b-12d3-a456-426614174000&userId=123e4567-e89b-12d3-a456-426614174001
```

## Ví dụ Response (Có OCR text)

```
Chat History Test ✅
Conversation ID: 123e4567-e89b-12d3-a456-426614174000
User ID: 123e4567-e89b-12d3-a456-426614174001
Exclude Message ID: null
Thời gian: 45ms
Số dòng: 12
Số ký tự: 856
Có OCR text: Có ✅

--- CHAT HISTORY ---
Người dùng: Hãy giải thích về bài thực hành 6

[Câu hỏi bổ sung từ hình ảnh: BÀI THỰC HÀNH 6
GV: Nguyễn Thị Minh Tâm
1. Đọc ảnh có các đường nét
2. Tách đối tượng khỏi nền
3. Lưu ảnh kết quả]

Trợ lý: Bài thực hành 6 là một bài tập về xử lý ảnh. Các bước thực hiện bao gồm:
1. Đọc ảnh có các đường nét
2. Tách đối tượng khỏi nền
3. Lưu ảnh kết quả

Người dùng: Cho tôi xem code mẫu

Trợ lý: Dưới đây là code mẫu cho bài thực hành 6:

```python
import cv2
import numpy as np

# Đọc ảnh
img = cv2.imread('input.jpg')

# Tách đối tượng khỏi nền
# ... code xử lý ...
```

Người dùng: Giải thích thêm về bước 2

Trợ lý: Bước 2 - Tách đối tượng khỏi nền là quá trình sử dụng các kỹ thuật như:
- Thresholding
- Contour detection
- Masking
...
```

## Ví dụ Response (Không có OCR text)

```
Chat History Test ✅
Conversation ID: 123e4567-e89b-12d3-a456-426614174000
User ID: 123e4567-e89b-12d3-a456-426614174001
Exclude Message ID: null
Thời gian: 32ms
Số dòng: 6
Số ký tự: 234
Có OCR text: Không ❌

--- CHAT HISTORY ---
Người dùng: Xin chào, bạn có khỏe không?

Trợ lý: Xin chào! Tôi khỏe, cảm ơn bạn đã hỏi. Bạn cần tôi giúp gì không?

Người dùng: Bạn có thể giải thích về AI không?

Trợ lý: AI (Artificial Intelligence) là trí tuệ nhân tạo, một lĩnh vực khoa học máy tính...
```

## Ví dụ Response (Chat history rỗng)

```
Chat History Test ✅
Conversation ID: 123e4567-e89b-12d3-a456-426614174000
User ID: 123e4567-e89b-12d3-a456-426614174001
Exclude Message ID: null
Thời gian: 15ms
Kết quả: Chat history rỗng (không có messages hoặc không có dữ liệu)
```

## Ví dụ Response (Có nhiều messages với OCR)

```
Chat History Test ✅
Conversation ID: 123e4567-e89b-12d3-a456-426614174000
User ID: 123e4567-e89b-12d3-a456-426614174001
Exclude Message ID: null
Thời gian: 78ms
Số dòng: 28
Số ký tự: 2456
Có OCR text: Có ✅

--- CHAT HISTORY ---
Người dùng: Hãy giải thích về bài thực hành 6

[Câu hỏi bổ sung từ hình ảnh: BÀI THỰC HÀNH 6
GV: Nguyễn Thị Minh Tâm
1. Đọc ảnh có các đường nét
2. Tách đối tượng khỏi nền
3. Lưu ảnh kết quả]

Trợ lý: Bài thực hành 6 là một bài tập về xử lý ảnh. Các bước thực hiện bao gồm:
1. Đọc ảnh có các đường nét
2. Tách đối tượng khỏi nền
3. Lưu ảnh kết quả

Người dùng: Cho tôi xem code mẫu

[Câu hỏi bổ sung từ hình ảnh: Code Python sử dụng OpenCV để xử lý ảnh]

Trợ lý: Dưới đây là code mẫu cho bài thực hành 6:

```python
import cv2
import numpy as np

# Đọc ảnh
img = cv2.imread('input.jpg')

# Tách đối tượng khỏi nền
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
_, mask = cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)

# Lưu ảnh kết quả
cv2.imwrite('output.jpg', mask)
```

Người dùng: Giải thích thêm về bước 2

Trợ lý: Bước 2 - Tách đối tượng khỏi nền là quá trình sử dụng các kỹ thuật như:
- Thresholding: Phân loại pixel thành foreground và background
- Contour detection: Tìm đường viền của đối tượng
- Masking: Tạo mask để tách đối tượng

Người dùng: Cảm ơn bạn

Trợ lý: Không có gì! Nếu bạn cần thêm thông tin, cứ hỏi tôi nhé!
```

## Ví dụ Response (Có excludeMessageId)

```
Chat History Test ✅
Conversation ID: 123e4567-e89b-12d3-a456-426614174000
User ID: 123e4567-e89b-12d3-a456-426614174001
Exclude Message ID: 123e4567-e89b-12d3-a456-426614174002
Thời gian: 38ms
Số dòng: 8
Số ký tự: 456
Có OCR text: Có ✅

--- CHAT HISTORY ---
Người dùng: Hãy giải thích về bài thực hành 6

[Câu hỏi bổ sung từ hình ảnh: BÀI THỰC HÀNH 6
GV: Nguyễn Thị Minh Tâm]

Trợ lý: Bài thực hành 6 là một bài tập về xử lý ảnh...

Người dùng: Cho tôi xem code mẫu

Trợ lý: Dưới đây là code mẫu cho bài thực hành 6...
```

## Ví dụ Response (Lỗi)

```
Chat History Test LỖI ❌: Conversation not found: 123e4567-e89b-12d3-a456-426614174000
Error Type: RuntimeException
Stack: com.example.springboot_api.services.user.ChatBotService.getChatHistoryInternal(ChatBotService.java:580)
```

## Format chi tiết

### Phần Header
- **Chat History Test ✅**: Trạng thái test
- **Conversation ID**: UUID của conversation
- **User ID**: UUID của user
- **Exclude Message ID**: UUID của message bị exclude (null nếu không có)
- **Thời gian**: Thời gian thực thi (ms)
- **Số dòng**: Số dòng trong chat history
- **Số ký tự**: Tổng số ký tự trong chat history
- **Có OCR text**: Có hoặc Không (với emoji ✅ hoặc ❌)

### Phần Chat History
- **Người dùng**: Câu hỏi của user
  - Nếu có OCR text: `[Câu hỏi bổ sung từ hình ảnh: ...]` hoặc `[Câu hỏi từ hình ảnh: ...]`
- **Trợ lý**: Câu trả lời của assistant
  - Nếu có OCR text: `[Thông tin từ hình ảnh: ...]`

### Lưu ý
- Nếu chat history quá dài (> 1002000 ký tự), sẽ bị truncate và hiển thị `...[truncated, total length: X chars]`
- Format `\n\n` được giữ nguyên để phân tách các message
- OCR text được format rõ ràng để LLM hiểu đây là phần câu hỏi bổ sung từ hình ảnh


