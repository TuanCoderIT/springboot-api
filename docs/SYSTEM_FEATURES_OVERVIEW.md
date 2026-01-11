# Hệ Thống Notebook & Exam Online - Tổng Quan Chức Năng

## 🎯 **Mục Đích Hệ Thống**
Hệ thống quản lý tài liệu học tập và thi trực tuyến với AI hỗ trợ, dành cho giảng viên và sinh viên.

---

## 👥 **Phân Quyền Người Dùng**

### **Admin**
- Quản lý người dùng, giảng viên, sinh viên
- Quản lý khoa, ngành, môn học, học kỳ
- Duyệt files upload trong community notebooks
- Quản lý quy định và thông báo hệ thống

### **Lecturer (Giảng viên)**
- Quản lý lớp học và sinh viên
- Upload và quản lý tài liệu học tập
- Tạo đề thi bằng AI từ tài liệu
- Quản lý kỳ thi và xem kết quả

### **Student (Sinh viên)**
- Tham gia notebooks, upload tài liệu
- Sử dụng AI tools để học tập
- Làm bài thi trực tuyến
- Chat với AI về tài liệu

---

## 📚 **Chức Năng Notebook System**

### **Notebook Management**
- Tạo và quản lý notebooks (cá nhân, lớp học, cộng đồng)
- Upload files (PDF, Word) với AI processing
- Phân quyền thành viên và duyệt files

### **AI Content Generation**
- **Quiz**: Tạo câu hỏi trắc nghiệm từ tài liệu
- **Flashcards**: Tạo thẻ ghi nhớ
- **Summary**: Tóm tắt nội dung tài liệu
- **Mindmap**: Tạo sơ đồ tư duy
- **Audio**: Chuyển text thành giọng nói
- **Video**: Tạo video giảng dạy
- **Suggestions**: Gợi ý học tập

### **Chat & Interaction**
- Chat với AI về nội dung tài liệu
- Bot chat hỗ trợ học tập
- Chat về quy định trường học

---

## 🎓 **Chức Năng Exam System**

### **Exam Creation (Lecturer)**
- Tạo đề thi từ tài liệu bằng AI
- Cấu hình câu hỏi (MCQ, True/False, Essay)
- Thiết lập thời gian, độ khó, số câu
- Preview và publish đề thi

### **Exam Taking (Student)**
- Xem danh sách bài thi có thể làm
- Làm bài thi với timer và bảo mật
- Chống gian lận (disable copy/paste, track tab switch)
- Auto-submit khi hết thời gian

### **Exam Management**
- Theo dõi tiến độ làm bài
- Chấm điểm tự động
- Xuất kết quả Excel/CSV
- Thống kê và phân tích

---

## 🔧 **Chức Năng Quản Lý**

### **Class Management**
- Tạo và quản lý lớp học
- Thêm/xóa sinh viên (manual hoặc import Excel)
- Phân quyền và theo dõi hoạt động

### **File Processing**
- OCR và embedding tự động
- Chunk processing cho AI
- Storage management
- Status tracking (pending, approved, done, failed)

### **Assignment & Chapter**
- Tạo bài tập và chương học
- Quản lý nội dung theo cấu trúc
- Theo dõi tiến độ học tập

---

## 🤖 **AI Integration**

### **AI Models**
- **Gemini**: Tạo nội dung, chat, Q&A
- **Groq**: Xử lý nhanh
- **Google Search**: Tìm kiếm bổ sung
- **TTS**: Text-to-Speech

### **AI Processing Pipeline**
1. Upload file → OCR extraction
2. Text chunking với overlap
3. Vector embedding
4. AI content generation
5. Result caching và storage

---

## 🔐 **Bảo Mật & Authentication**

### **JWT Authentication**
- Role-based access control
- Token refresh mechanism
- Session management

### **Exam Security**
- Browser lockdown mode
- Activity monitoring
- IP tracking
- Academic integrity checks

---

## 📊 **Database Structure**

### **Core Entities**
- Users, Lecturers, Students
- Notebooks, Files, Chunks
- Exams, Questions, Attempts
- Classes, Subjects, Terms

### **AI Entities**
- NotebookAiSet (AI generation tracking)
- Quiz, Flashcard, Summary, etc.
- Chat conversations
- Processing tasks

---

## 🌐 **API Architecture**

### **RESTful APIs**
- `/admin/*` - Admin functions
- `/lecturer/*` - Lecturer functions  
- `/user/*` - Student functions
- `/api/exams` - Exam system
- `/shared/*` - Common functions

### **File Upload**
- Multipart form-data
- Validation và processing
- Storage management
- Progress tracking

---

## 🚀 **Technology Stack**

### **Backend**
- Spring Boot 3.x
- PostgreSQL database
- JWT security
- File storage system

### **AI Integration**
- Google Gemini API
- Groq API
- Vector embeddings
- Text processing

### **Frontend (Planned)**
- Next.js React
- TypeScript
- Responsive design
- Real-time updates

---

## 📈 **Key Features Summary**

✅ **Document Management**: Upload, process, organize learning materials  
✅ **AI-Powered Learning**: Generate quizzes, summaries, flashcards from documents  
✅ **Online Exams**: Create and take exams with AI-generated questions  
✅ **Class Management**: Manage students, assignments, and progress  
✅ **Chat & Interaction**: AI chat about documents and regulations  
✅ **Security & Monitoring**: Exam proctoring and academic integrity  
✅ **Multi-Role System**: Admin, lecturer, and student interfaces  
✅ **Scalable Architecture**: RESTful APIs and modular design