-- ============================================================================
-- 动作库升级：增加器械分类与动作要点字段（Keep式动作选择器的数据基础）
-- ============================================================================
USE fitness_db;
SET NAMES utf8mb4;

ALTER TABLE exercise_lib
    ADD COLUMN equipment VARCHAR(20)  NOT NULL DEFAULT '自重' COMMENT '器械: 杠铃/哑铃/器械/自重/有氧' AFTER muscle_group,
    ADD COLUMN tips      VARCHAR(600) NULL COMMENT '动作要点(文本讲解)' AFTER equipment,
    ADD COLUMN image_url VARCHAR(500) NULL COMMENT '插图地址(预留扩展)' AFTER tips;

UPDATE exercise_lib SET equipment = '杠铃', tips = '平躺凳面，双脚踩实，杠铃对准乳头上方，下放至触胸后推起，全程肩胛收紧' WHERE name = '杠铃卧推';
UPDATE exercise_lib SET equipment = '哑铃', tips = '哑铃下放至胸部两侧，推起时保持手腕中立，顶部不要互相碰撞' WHERE name = '哑铃卧推';
UPDATE exercise_lib SET equipment = '哑铃', tips = '凳面调高30-45度，侧重上胸，下放深度充分再推起' WHERE name = '上斜哑铃卧推';
UPDATE exercise_lib SET equipment = '哑铃', tips = '手臂微屈如环抱大树，放低至与肩平齐，用胸肌收缩带动合拢' WHERE name = '哑铃飞鸟';
UPDATE exercise_lib SET equipment = '自重', tips = '身体一条直线，核心收紧，下放时胸部贴近地面，手掌在胸两侧' WHERE name = '俯卧撑';
UPDATE exercise_lib SET equipment = '自重', tips = '身体前倾，屈肘下放至大臂平行地面，主要靠胸部发力推起' WHERE name = '双杠臂屈伸';
UPDATE exercise_lib SET equipment = '器械', tips = '调节座椅使把手与胸中部齐平，推起时不过度锁死肘关节' WHERE name = '绳索夹胸';
UPDATE exercise_lib SET equipment = '自重', tips = '全程核心收紧不摆动，上拉至下巴过杠，下放控制速度' WHERE name = '引体向上';
UPDATE exercise_lib SET equipment = '杠铃', tips = '俯身约45度，背部平直，杠铃沿大腿拉向肚脐，顶峰收缩1秒' WHERE name = '杠铃划船';
UPDATE exercise_lib SET equipment = '哑铃', tips = '单手持哑铃，背部挺直，肘部贴身上拉至腰侧，感受背阔肌收缩' WHERE name = '哑铃单臂划船';
UPDATE exercise_lib SET equipment = '器械', tips = '宽握距，下拉至锁骨位置，回放控制不要耸肩' WHERE name = '高位下拉';
UPDATE exercise_lib SET equipment = '器械', tips = '双脚蹬实踏板，挺胸拉向腹部，回放不弓腰' WHERE name = '坐姿划船';
UPDATE exercise_lib SET equipment = '器械', tips = '手臂基本伸直，用背阔肌力量下压至大腿前侧，像拥抱大树' WHERE name = '直臂下压';
UPDATE exercise_lib SET equipment = '自重', tips = '俯卧挺身至身体一条线即可，不过度后仰，核心收紧' WHERE name = '山羊挺身';
UPDATE exercise_lib SET equipment = '杠铃', tips = '站距与肩同宽，先屈髋后屈膝，下蹲至大腿至少平行地面，膝盖对准脚尖' WHERE name = '深蹲';
UPDATE exercise_lib SET equipment = '杠铃', tips = '杠铃置于锁骨前，核心收紧躯干直立，下蹲深度充分' WHERE name = '前蹲';
UPDATE exercise_lib SET equipment = '自重', tips = '躯干直立，前腿膝盖不超过脚尖，后腿膝盖轻触地面即起' WHERE name = '箭步蹲';
UPDATE exercise_lib SET equipment = '杠铃', tips = '屈髋为主膝盖微屈，杠铃贴腿下放至腘绳肌有牵拉感即拉起' WHERE name = '罗马尼亚硬拉';
UPDATE exercise_lib SET equipment = '器械', tips = '双脚与髋同宽踩实，下放至90度左右蹬起，不锁死膝盖' WHERE name = '腿举';
UPDATE exercise_lib SET equipment = '器械', tips = '调节轴心与膝关节对齐，抬腿至接近伸直不过伸' WHERE name = '腿屈伸';
UPDATE exercise_lib SET equipment = '器械', tips = '俯卧收紧腘绳肌弯举，下放控制速度不弹震' WHERE name = '腿弯举';
UPDATE exercise_lib SET equipment = '自重', tips = '前脚掌踩实踏板，缓慢提踵至最高，顶端停顿再下放' WHERE name = '站姿提踵';
UPDATE exercise_lib SET equipment = '哑铃', tips = '从耳侧推起至头顶，核心收紧不塌腰，下放至耳侧' WHERE name = '哑铃推举';
UPDATE exercise_lib SET equipment = '杠铃', tips = '从锁骨前推起至头顶，躯干稳定，颈前或颈后量力而行' WHERE name = '杠铃推举';
UPDATE exercise_lib SET equipment = '哑铃', tips = '微屈肘向两侧抬起至肩高，像倒水一样，感受中束发力' WHERE name = '侧平举';
UPDATE exercise_lib SET equipment = '哑铃', tips = '交替或同步前平举至眼平高度，控制回落速度' WHERE name = '前平举';
UPDATE exercise_lib SET equipment = '哑铃', tips = '俯身约45度，向两侧张开手臂，感受后束收缩' WHERE name = '俯身飞鸟';
UPDATE exercise_lib SET equipment = '器械', tips = '绳索高位向眉心方向拉，上臂与地面平行，改善圆肩' WHERE name = '面拉';
UPDATE exercise_lib SET equipment = '杠铃', tips = '大臂固定贴身体两侧，前臂弯举至肩高，顶端收缩' WHERE name = '杠铃弯举';
UPDATE exercise_lib SET equipment = '哑铃', tips = '掌心向前，弯举时上臂不摆动，下放全程控制' WHERE name = '哑铃弯举';
UPDATE exercise_lib SET equipment = '哑铃', tips = '掌心相对如握锤，兼顾肱肌与前臂，动作缓慢稳定' WHERE name = '锤式弯举';
UPDATE exercise_lib SET equipment = '自重', tips = '身体贴近凳缘，屈肘下放至90度再撑起，躯干直上直下' WHERE name = '凳上臂屈伸';
UPDATE exercise_lib SET equipment = '器械', tips = '上臂固定于体侧，用三头肌下压至手臂伸直，顶峰收缩' WHERE name = '绳索下压';
UPDATE exercise_lib SET equipment = '杠铃', tips = '握距略窄于肩，下放至触胸再推起，侧重三头和内胸' WHERE name = '窄距卧推';
UPDATE exercise_lib SET equipment = '自重', tips = '前臂支撑，身体一条直线，核心收紧不塌腰不翘臀' WHERE name = '平板支撑';
UPDATE exercise_lib SET equipment = '自重', tips = '下巴微收，用腹肌卷起肩胛离地即可，腰部始终贴地' WHERE name = '卷腹';
UPDATE exercise_lib SET equipment = '自重', tips = '悬挂抬膝至骨盆后倾，避免单纯摆腿，感受下腹发力' WHERE name = '悬垂举腿';
UPDATE exercise_lib SET equipment = '自重', tips = '坐姿后仰45度，转体时胸椎带动，核心持续收紧' WHERE name = '俄罗斯转体';
UPDATE exercise_lib SET equipment = '自重', tips = '对侧手脚缓慢伸展，腰部贴地，练深层核心稳定性' WHERE name = '死虫式';
UPDATE exercise_lib SET equipment = '有氧', tips = '配速以能完整说一句话为宜，前5分钟热身，循序渐进' WHERE name = '慢跑';
UPDATE exercise_lib SET equipment = '有氧', tips = '前脚掌轻触地，手腕摇绳不用大臂，分组进行' WHERE name = '跳绳';
UPDATE exercise_lib SET equipment = '有氧', tips = '俯身撑地跳回深蹲位再跳起，落地缓冲，量力而行' WHERE name = '波比跳';
UPDATE exercise_lib SET equipment = '有氧', tips = '30秒高强度+30秒间歇循环，动作标准优先于速度' WHERE name = 'HIIT循环';
UPDATE exercise_lib SET equipment = '器械', tips = '座椅高度使膝盖微屈，阻力适中，保持踏频稳定' WHERE name = '动感单车';
UPDATE exercise_lib SET equipment = '器械', tips = '蹬腿→核心后仰→手臂后拉，各部位连贯发力' WHERE name = '划船机';
UPDATE exercise_lib SET equipment = '有氧', tips = '自由泳或蛙泳均可，保持规律呼吸，持续30分钟以上' WHERE name = '游泳';
