import os
import cv2
import random
import numpy as np


# 矩形框扩大
# rect 矩形框，x1,y1,x2,y2
# width 图像宽
# height 图像高
# ratio 扩大比例
# 返回扩大后的矩形框
def expand(rect, width, height, ratio):
    w = int((rect[2] - rect[0] + 1) * ratio)
    h = int((rect[3] - rect[1] + 1) * ratio)
    cx = (rect[2] + rect[0]) // 2
    cy = (rect[3] + rect[1]) // 2
    x1 = cx - w // 2
    y1 = cy - h // 2
    x2 = x1 + w - 1
    y2 = y1 + h - 1
    x1 = max(0, x1)
    x2 = min(width - 1, x2)
    y1 = max(0, y1)
    y2 = min(height - 1, y2)
    return np.array([x1, y1, x2, y2])


# 前景与背景融合
# fore 前景
# bg 背景
# 方法：与边界距离为0时，全部用背景，与边界距离大于等于dmax时，全部用前景。其他按比例过渡
# 返回融合后的图像块
def blend(fore, bg):
    out = np.zeros(fore.shape, dtype=np.uint8)
    out = fore
    h, w, _ = fore.shape
    dmax = min(w, h) // 4
    for i in range(out.shape[0]):
        for j in range(out.shape[1]):
            dist = np.array([i, h - 1 - i, j, w - 1 - j])
            d = dist.min()
            r = d / dmax
            if r < 1:
                out[i, j, :] = (fore[i, j, :] * r + bg[i, j, :] * (1 - r)).astype(np.uint8)
    return out


# 合成图像
# rects 矩形框，为ndarray格式，维度为(n, 4)
# fore_img 前景图片，为ndarray格式
# back_img 背景图片，为ndarray格式
# pixel_range 合成目标的像素宽度范围
# output_name 输出图片名称
def mix(rects, fore_img, back_img, pixel_range, output_name):
    #k = random.randint(0, rects.shape[0] - 1)  # 随机选一个目标
    k = 0  # todo: 此处应该随机选一个目标，但不能单独选到小数点，因此暂时固定选第一个目标
    for i in range(rects.shape[0]):
        rect = rects[i]
        if i != k:
            fore_img[rect[1]:rect[3], rect[0]:rect[2], :] = 0  # 其他目标涂黑
    target_size = random.randint(pixel_range[0], pixel_range[1])  # 随机取一个目标大小
    r = target_size / (rects[k, 2] - rects[k, 0] + 1)  # 缩放比例
    fore_img = cv2.resize(fore_img, (int(fore_img.shape[1] * r), int(fore_img.shape[0] * r)))
    #cv2.imshow('fore', fore_img)
    #cv2.waitKey()
    rect = (rects[k] * r).astype(np.int32)
    expand_rec = expand(rect, fore_img.shape[1], fore_img.shape[0], random.uniform(2.0, 3.0))  #把矩形框扩大随机倍数
    x = expand_rec[0]
    y = expand_rec[1]
    w = expand_rec[2] - x + 1
    h = expand_rec[3] - y + 1

    if back_img.shape[1] < w or back_img.shape[0] < h:
        return
    target_x = random.randint(0, back_img.shape[1] - w)
    target_y = random.randint(0, back_img.shape[0] - h)
    out = blend(fore_img[y: y + h, x:x + w, :], back_img[target_y: target_y + h, target_x: target_x + w, :])
    back_img[target_y: target_y + h, target_x: target_x + w, :] = out
    # todo: 此处还应输出以下标签
    x1 = target_x + rect[0] - expand_rec[0]  # xmin
    y1 = target_y + rect[1] - expand_rec[1]  # ymin
    x2 = x1 + rect[2]  # xmax
    y2 = y1 + rect[3]  # ymax
    cv2.imwrite(output_name, back_img)


# 读txt标注文件，输出为numpy ndarray格式
def read_label_txt(txt_name, fore_img):
    f = open(txt_name, 'rt')
    items = f.readlines()
    rects = np.zeros((len(items), 4), dtype=np.int32)
    for i, item in enumerate(items):
        item = item.strip().split()
        item_ = [float(j) for j in item[1:5]]
        x1 = int(fore_img.shape[1] * (item_[0] - item_[2] * 0.5) + 0.5)
        y1 = int(fore_img.shape[0] * (item_[1] - item_[3] * 0.5) + 0.5)
        w = int(fore_img.shape[1] * item_[2] + 0.5)
        h = int(fore_img.shape[0] * item_[3] + 0.5)
        x2 = x1 + w
        y2 = y1 + h
        rects[i, 0:4] = np.array([x1, y1, x2, y2])
    return rects

if __name__ == '__main__':
    train_data_path = r'D:\data\ocr\biaozhu' # 训练图像目录，必须有img和txt子目录
    background_data_path = r'bg' # 背景图目录
    output_data_path = r'D:\data\ocr\output' # 输出合成图像目录
    pixel_range = [32, 128] # 目标像素宽度范围，尽量与测试图像目标宽度范围一致

    os.makedirs(output_data_path, exist_ok=True)
    fg_files = os.listdir(os.path.join(train_data_path, 'txt'))

    # 对每张背景图生成一张合成图。todo: 是否需要把合成图缩放到统一大小？
    for i, bg_file in enumerate(os.listdir(background_data_path)):
        print(f'{i}, {bg_file}')
        fg_index = i % len(fg_files) # 选一张前景图，可以随机选
        fg_img_name = fg_files[fg_index][:-3] + 'jpg'
        fg_img = os.path.join(train_data_path, 'img', fg_img_name)
        output_img_name = os.path.join(output_data_path, bg_file[:-4] + '_' + fg_img_name)

        fore_img = cv2.imread(fg_img)
        back_img = cv2.imread(os.path.join(background_data_path, bg_file))

        # 读txt标注文件
        rects = read_label_txt(os.path.join(train_data_path, 'txt', fg_files[fg_index]), fore_img)

        mix(rects, fore_img, back_img, pixel_range, output_img_name)




