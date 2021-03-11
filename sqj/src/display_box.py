# coding:utf-8
"""
@ author: kkksqj
@ date: 2021/3/2

"""
import os

import cv2


def display(imgPath,annoPath):
    for i in os.listdir(annoPath):
        txtName = i
        jpgName = i[:-4]+'.jpg'
        img = cv2.imread(os.path.join(imgPath, jpgName))
        height,width = img.shape[:2]
        with open(os.path.join(annoPath,txtName),'r') as f:
            for line in f.readlines():
                label, cx, cy, cw,ch = line.strip().split()
                cx = float(cx)
                cy = float(cy)
                cw = float(cw)
                ch = float(ch)
                xmin = int(cx *width-cw*width/2)
                ymin = int(cy*height-ch*height/2)
                xmax = int(xmin+cw*width)
                ymax = int(ymin+ch*height)
                cv2.rectangle(img,(xmin,ymin),(xmax,ymax),(0,0,255),2)
        cv2.imshow("A",img)
        cv2.waitKey()
if __name__ == '__main__':
    """
    box可视化
    imgPath:图片绝对路径
    annoPath:标注信息绝对路径
    标注信息格式：yolo格式
    xx.txt
    label cx cy cw ch
    xmin = int(cx *width-cw*width/2)
    ymin = int(cy*height-ch*height/2)
    xmax = int(xmin+cw*width)
    ymax = int(ymin+ch*height)
    """
    imgPath = r'D:\practice\detection\Digit_recognition\data\train\images'
    annoPath = r'D:\practice\detection\Digit_recognition\data\train\labels'
    display(imgPath,annoPath)
