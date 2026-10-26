-- 003: Bộ câu hỏi v2 (13 câu, 5 nhóm)
--  * Thêm nhóm "Chi tiêu & đồ dùng chung".
--  * Thêm 5 câu: giờ thức dậy, chia việc nhà, chia tiền điện nước, máy lạnh, khách ngủ lại qua đêm.
--  * Trọng số: hút thuốc, thú cưng = 2; giờ ngủ, chia tiền = 1.5; còn lại = 1.
--  * Chép câu trả lời v1 sang v2 theo question_key: người đã làm bài chỉ cần trả lời 5 câu mới.
--  * v1 chuyển sang archived, v2 published.
-- v1 bị trigger khóa (đã phát hành) nên không sửa trực tiếp; mọi thay đổi đi qua version mới.
BEGIN;

INSERT INTO lifestyle_criteria (criterion_key, criterion_name, description, display_order)
VALUES ('expenses_shared', 'Chi tiêu & đồ dùng chung', 'Cách chia tiền điện nước, máy lạnh và đồ dùng chung.', 5)
ON CONFLICT (criterion_key) DO NOTHING;

INSERT INTO questionnaire_versions (version_key, version_name, description, status)
VALUES ('v2', 'Bộ câu hỏi Roomie v2',
        '13 câu hỏi, thêm nhóm chi tiêu chung và các câu về giờ thức dậy, việc nhà, khách ngủ lại.', 'draft')
ON CONFLICT (version_key) DO NOTHING;

-- Câu hỏi: thứ tự đi theo nhóm để bài test liền mạch
INSERT INTO lifestyle_questions
    (questionnaire_version_id, criterion_id, question_key, question_text, room_question_text, weight, display_order)
SELECT v.questionnaire_version_id, c.criterion_id, x.question_key, x.question_text, x.room_question_text, x.weight, x.display_order
FROM questionnaire_versions v
CROSS JOIN (VALUES
    ('sleep_noise',        'sleep_time',       'Bạn thường đi ngủ lúc mấy giờ?',
                                               'Các thành viên trong phòng thường đi ngủ lúc mấy giờ?', 1.5, 1),
    ('sleep_noise',        'wake_time',        'Bạn thường thức dậy lúc mấy giờ?',
                                               'Các thành viên trong phòng thường thức dậy lúc mấy giờ?', 1.0, 2),
    ('sleep_noise',        'noise',            'Mức độ nhạy cảm với tiếng ồn của bạn?',
                                               'Phòng cần mức yên tĩnh như thế nào?', 1.0, 3),
    ('cleanliness_chores', 'cleanliness',      'Bạn thích không gian sạch sẽ đến mức nào?',
                                               'Phòng giữ mức độ sạch sẽ như thế nào?', 1.0, 4),
    ('cleanliness_chores', 'chores',           'Bạn muốn chia việc nhà như thế nào?',
                                               'Phòng đang chia việc nhà như thế nào?', 1.0, 5),
    ('cleanliness_chores', 'cooking',          'Tần suất nấu ăn ở nhà của bạn?',
                                               'Phòng có thường nấu ăn ở nhà không?', 1.0, 6),
    ('smoking_pets',       'smoking',          'Bạn có hút thuốc không?',
                                               'Quy định về hút thuốc của phòng là gì?', 2.0, 7),
    ('smoking_pets',       'pets',             'Góc nhìn của bạn về nuôi thú cưng?',
                                               'Phòng có cho nuôi thú cưng không?', 2.0, 8),
    ('expenses_shared',    'bills_split',      'Bạn muốn chia tiền điện nước, máy lạnh như thế nào?',
                                               'Phòng đang chia tiền điện nước, máy lạnh như thế nào?', 1.5, 9),
    ('expenses_shared',    'air_conditioning', 'Bạn thường bật máy lạnh khi nào?',
                                               'Phòng thường bật máy lạnh khi nào?', 1.0, 10),
    ('guests_social',      'guests',           'Mức độ dẫn bạn bè về phòng chơi?',
                                               'Phòng có thoải mái với việc dẫn bạn bè về chơi không?', 1.0, 11),
    ('guests_social',      'overnight_guests', 'Bạn thấy thế nào khi khách hoặc người yêu ngủ lại qua đêm?',
                                               'Phòng có cho khách ngủ lại qua đêm không?', 1.0, 12),
    ('guests_social',      'communication',    'Phong cách giao tiếp bạn hướng tới?',
                                               'Các thành viên trong phòng giao tiếp với nhau thế nào?', 1.0, 13)
) AS x(criterion_key, question_key, question_text, room_question_text, weight, display_order)
JOIN lifestyle_criteria c ON c.criterion_key = x.criterion_key
WHERE v.version_key = 'v2' AND v.status = 'draft'
ON CONFLICT (questionnaire_version_id, question_key) DO NOTHING;

-- Lựa chọn: score = thứ tự trên thang (1..3), dùng cho thuật toán tương thích
INSERT INTO lifestyle_question_options
    (question_id, option_key, option_label, room_option_label, score, display_order)
SELECT q.question_id, o.option_key, o.option_label, o.room_option_label, o.display_order, o.display_order
FROM lifestyle_questions q
JOIN questionnaire_versions v ON v.questionnaire_version_id = q.questionnaire_version_id
JOIN (VALUES
    -- 8 câu giữ nguyên từ v1
    ('sleep_time', 'before_23',       'Trước 23:00',                          NULL, 1),
    ('sleep_time', 'from_23_to_24',   'Từ 23:00 đến 00:00',                   NULL, 2),
    ('sleep_time', 'after_24',        'Sau 00:00, mình là cú đêm',            'Sau 00:00', 3),
    ('cleanliness', 'daily',          'Gọn gàng, dọn dẹp mỗi ngày',           NULL, 1),
    ('cleanliness', 'few_times_week', 'Dọn vài lần mỗi tuần',                 NULL, 2),
    ('cleanliness', 'relaxed',        'Thoải mái, bừa một chút cũng ổn',      NULL, 3),
    ('smoking', 'no_smoke_free',      'Không hút, muốn phòng không khói',     'Không ai hút, phòng không khói', 1),
    ('smoking', 'no_outside_ok',      'Không hút, chấp nhận hút bên ngoài',   'Chỉ được hút bên ngoài', 2),
    ('smoking', 'smoker_allowed_areas','Có hút, chỉ hút ở nơi cho phép',      'Có người hút ở nơi cho phép', 3),
    ('cooking', 'daily',              'Nấu ăn mỗi ngày, thích tự nấu',        'Nấu ăn mỗi ngày', 1),
    ('cooking', 'few_times_week',     'Nấu vài lần tuần khi có thời gian',    'Nấu vài lần mỗi tuần', 2),
    ('cooking', 'rarely',             'Hầu như đặt đồ ăn ngoài',              'Hầu như không nấu', 3),
    ('guests', 'social',              'Thoải mái, thích không gian nhộn nhịp', NULL, 1),
    ('guests', 'notify_first',        'Cần báo trước cho bạn cùng phòng',     NULL, 2),
    ('guests', 'limited',             'Hạn chế, thích không gian riêng tư',   NULL, 3),
    ('pets', 'has_pet',               'Rất yêu thích và đang nuôi thú cưng',  'Phòng đang nuôi thú cưng', 1),
    ('pets', 'ok_if_clean',           'Đồng ý nuôi nếu sạch sẽ và không ồn',  'Được nuôi nếu sạch sẽ và không ồn', 2),
    ('pets', 'no_pets',               'Không muốn ở cùng thú cưng',           'Không nuôi thú cưng', 3),
    ('noise', 'very_quiet',           'Cần không gian yên tĩnh tuyệt đối',    NULL, 1),
    ('noise', 'moderate',             'Bình thường, chấp nhận tiếng ồn vừa phải', NULL, 2),
    ('noise', 'tolerant',             'Dễ tính, không ảnh hưởng bởi tiếng ồn', NULL, 3),
    ('communication', 'close',        'Thích trò chuyện, chia sẻ như bạn thân', NULL, 1),
    ('communication', 'friendly_private', 'Thân thiện nhưng giữ không gian riêng', NULL, 2),
    ('communication', 'private',      'Tôn trọng sự riêng tư, ít giao tiếp',  NULL, 3),
    -- 5 câu mới
    ('wake_time', 'before_7',         'Trước 7:00, mình dậy sớm',             'Trước 7:00', 1),
    ('wake_time', 'from_7_to_9',      'Từ 7:00 đến 9:00',                     NULL, 2),
    ('wake_time', 'after_9',          'Sau 9:00, mình thích ngủ nướng',       'Sau 9:00', 3),
    ('chores', 'schedule',            'Lập lịch trực rõ ràng theo tuần',      'Có lịch trực rõ ràng theo tuần', 1),
    ('chores', 'self_aware',          'Ai rảnh thì làm, mọi người tự giác',   'Ai rảnh thì làm, tự giác', 2),
    ('chores', 'hire_cleaner',        'Thuê người dọn dẹp, chia tiền',        'Thuê người dọn dẹp, chia tiền', 3),
    ('bills_split', 'equal',          'Chia đều tất cả các khoản',            'Chia đều tất cả các khoản', 1),
    ('bills_split', 'flexible',       'Thỏa thuận linh hoạt từng tháng',      'Thỏa thuận linh hoạt từng tháng', 2),
    ('bills_split', 'by_usage',       'Chia theo mức dùng (đồng hồ riêng)',   'Tính theo đồng hồ riêng từng người', 3),
    ('air_conditioning', 'all_day',   'Bật gần như cả ngày',                  'Bật gần như cả ngày', 1),
    ('air_conditioning', 'night_only','Chỉ bật khi ngủ buổi tối',             'Chỉ bật buổi tối', 2),
    ('air_conditioning', 'rarely',    'Hiếm khi, mình quen dùng quạt',        'Hiếm khi bật, chủ yếu dùng quạt', 3),
    ('overnight_guests', 'comfortable','Thoải mái, không vấn đề gì',          'Thoải mái', 1),
    ('overnight_guests', 'notify_rarely','Được, nếu báo trước và không thường xuyên', 'Được, nếu báo trước và không thường xuyên', 2),
    ('overnight_guests', 'not_allowed','Không đồng ý khách ngủ lại',          'Không cho khách ngủ lại', 3)
) AS o(question_key, option_key, option_label, room_option_label, display_order)
  ON o.question_key = q.question_key
WHERE v.version_key = 'v2' AND v.status = 'draft'
ON CONFLICT (question_id, option_key) DO NOTHING;

-- Chép câu trả lời v1 -> v2 cho các câu giữ nguyên (cùng question_key và option_key còn tồn tại)
INSERT INTO user_lifestyle_answers (user_id, questionnaire_version_id, question_id, answer_value)
SELECT a.user_id, q2.questionnaire_version_id, q2.question_id, a.answer_value
  FROM user_lifestyle_answers a
  JOIN lifestyle_questions q1 ON q1.question_id = a.question_id
  JOIN questionnaire_versions v1 ON v1.questionnaire_version_id = q1.questionnaire_version_id AND v1.version_key = 'v1'
  JOIN questionnaire_versions v2 ON v2.version_key = 'v2'
  JOIN lifestyle_questions q2 ON q2.questionnaire_version_id = v2.questionnaire_version_id
                             AND q2.question_key = q1.question_key
  JOIN lifestyle_question_options o2 ON o2.question_id = q2.question_id
                                    AND o2.option_key = a.answer_value->>'option_key'
ON CONFLICT DO NOTHING;

INSERT INTO post_lifestyle_answers (post_id, questionnaire_version_id, question_id, answer_value)
SELECT a.post_id, q2.questionnaire_version_id, q2.question_id, a.answer_value
  FROM post_lifestyle_answers a
  JOIN lifestyle_questions q1 ON q1.question_id = a.question_id
  JOIN questionnaire_versions v1 ON v1.questionnaire_version_id = q1.questionnaire_version_id AND v1.version_key = 'v1'
  JOIN questionnaire_versions v2 ON v2.version_key = 'v2'
  JOIN lifestyle_questions q2 ON q2.questionnaire_version_id = v2.questionnaire_version_id
                             AND q2.question_key = q1.question_key
  JOIN lifestyle_question_options o2 ON o2.question_id = q2.question_id
                                    AND o2.option_key = a.answer_value->>'option_key'
ON CONFLICT DO NOTHING;

-- Tiến độ v2: ai đã bắt đầu v1 thì làm tiếp từ câu đầu tiên chưa trả lời của v2
INSERT INTO user_lifestyle_test_progress (user_id, questionnaire_version_id, current_step, is_completed, started_at)
SELECT p.user_id, v2.questionnaire_version_id,
       COALESCE((SELECT MIN(q.display_order) FROM lifestyle_questions q
                  WHERE q.questionnaire_version_id = v2.questionnaire_version_id AND q.is_active
                    AND NOT EXISTS (SELECT 1 FROM user_lifestyle_answers a
                                     WHERE a.user_id = p.user_id AND a.question_id = q.question_id)), 1),
       FALSE, p.started_at
  FROM user_lifestyle_test_progress p
  JOIN questionnaire_versions v1 ON v1.questionnaire_version_id = p.questionnaire_version_id AND v1.version_key = 'v1'
  JOIN questionnaire_versions v2 ON v2.version_key = 'v2'
ON CONFLICT DO NOTHING;

-- Phát hành: chỉ được có 1 version published nên archive v1 trước
UPDATE questionnaire_versions SET status = 'archived'
 WHERE version_key = 'v1' AND status = 'published';
UPDATE questionnaire_versions SET status = 'published', published_at = NOW()
 WHERE version_key = 'v2' AND status = 'draft';

COMMIT;
