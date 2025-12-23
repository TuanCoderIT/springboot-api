-- =====================================================
-- Add visibility flags to chapter_items
-- =====================================================

-- Thêm 2 trường boolean để kiểm soát hiển thị
ALTER TABLE chapter_items
ADD COLUMN IF NOT EXISTS visible_in_lesson BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS visible_in_notebook BOOLEAN NOT NULL DEFAULT true;

-- Tạo index cho query hiệu quả hơn
CREATE INDEX IF NOT EXISTS idx_chapter_items_visibility 
ON chapter_items(visible_in_lesson, visible_in_notebook);

COMMENT ON COLUMN chapter_items.visible_in_lesson IS 'Hiển thị item trong bài học cho sinh viên';
COMMENT ON COLUMN chapter_items.visible_in_notebook IS 'Hiển thị item trong notebook khi ôn tập';
