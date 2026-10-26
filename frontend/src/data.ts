import type { Product, Trade } from './types';
const base = { imageUrls: [], status: 'ON_SALE' as const, version: 0, createdAt: '2026-10-21T09:00:00+08:00' };
export const seedProducts: Product[] = [
  { ...base, id: 101, sellerId: 2, sellerName: '林同学', title: 'MacBook Air 13 英寸', description: '轻一点，带走更多可能。M2 芯片，16GB 内存，256GB 储存。日常学习使用，屏幕无划痕，附原装充电器。可在图书馆当面验机。', price: '4299.00', category: '数码设备', condition: '九五新', art: 'laptop' },
  { ...base, id: 102, sellerId: 3, sellerName: '陈同学', title: '无线降噪耳机', description: '给专注，留一点安静。头戴式无线耳机，功能完好，耳罩已清洁，附收纳盒。', price: '680.00', category: '数码设备', condition: '九成新', art: 'headphones' },
  { ...base, id: 103, sellerId: 4, sellerName: '许同学', title: '富士 X100 复古相机', description: '把校园的每一天，拍成值得珍藏的画面。镜头通透，无霉无雾，附电池及肩带。', price: '3200.00', category: '数码设备', condition: '九成新', art: 'camera' },
  { ...base, id: 104, sellerId: 2, sellerName: '林同学', title: '设计与生活 · 书籍三册', description: '翻过的每一页，都有新的启发。设计入门书籍三册，无缺页，少量铅笔标注。', price: '65.00', category: '图书教材', condition: '九成新', art: 'books' },
  { ...base, id: 105, sellerId: 5, sellerName: '周同学', title: '城市通勤自行车', description: '从宿舍到教学楼，把沿途风景留给自己。车况良好，刹车灵敏，附车锁。', price: '360.00', category: '运动户外', condition: '八成新', art: 'bike' },
  { ...base, id: 106, sellerId: 1, sellerName: '我', title: '极简护眼台灯', description: '陪你度过每一个认真学习的夜晚。三档色温，支持无级调光，附原包装。', price: '89.00', category: '生活好物', condition: '九五新', art: 'lamp' },
  { ...base, id: 107, sellerId: 1, sellerName: '我', title: '高等数学学习笔记', description: '上下册配套学习资料，整洁清晰，可在教学楼交接。', price: '25.00', category: '图书教材', condition: '九成新', art: 'books', status: 'DRAFT' },
];
export const seedTrades: Trade[] = [{ id: 1001, productId: 106, buyerId: 2, sellerId: 1, message: '同学你好，明天下午可以在图书馆门口交接吗？', status: 'PENDING', createdAt: '2026-10-21T10:30:00+08:00', logs: [{ status: 'PENDING', at: '2026-10-21T10:30:00+08:00' }] }];
