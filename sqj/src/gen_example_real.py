# coding:utf-8
"""
@ author: kkksqj
@ date: 2021/3/1

"""
import os
import random
import xml.etree.ElementTree as ET
from collections import defaultdict
import shutil
import numpy as np
import cv2
from tqdm import tqdm

import warnings

warnings.filterwarnings('ignore')
"""
针对真实测试图片生成训练样本
"""




class gen_example():
    def __init__(self,img_path,anno_path,out_path):
        self.img_path = img_path
        self.anno_path = anno_path
        self.outImg_path = os.path.join(out_path,"images")    #样本输出目录
        self.outAnno_path = os.path.join(out_path,"labels")  #标注信息输出目录
        self.new_shape = (640, 640)  # 新图的w h
        self.color = (114, 114, 114)    # 填充的颜色
        self.obj_w = (32, 128)      #新图中目标w的范围，最小W=32，最大W=128
        self.per_numOfObj = 5    #每个目标随机生成N张图
        self.k=1                    #样本初始ID
        self.scaleup = True         #True:任意 False:只支持下采样
        self.check_before_run()
        self.gen_example()

    def check_before_run(self):
        if not os.path.exists(self.img_path):
            print("img path: {} is not available".format(self.img_path))
        if not os.path.exists(self.anno_path):
            print("anno path: {} is not available".format(self.anno_path))
        if os.path.exists(self.outImg_path):
            shutil.rmtree(self.outImg_path)
            print("remove img path:{}".format(self.outImg_path))
        os.makedirs(self.outImg_path)
        print("img out path: {}".format(self.outImg_path))
        if os.path.exists(self.outAnno_path):
            shutil.rmtree(self.outAnno_path)
            print("remove anno path:{}".format(self.outAnno_path))
        os.makedirs(self.outAnno_path)
        print("anno out path :{}".format(self.outAnno_path))


    def get_xml_list(self):
        anno_list = []
        for i in os.listdir(self.anno_path):
            if i.endswith(".xml"):
                anno_list.append(i)
        len_anno_list = len(anno_list)
        return anno_list,len_anno_list


    def read_xml(self,xml):
        tree = ET.parse(xml)
        root = tree.getroot()
        nodes_list = list(root)
        nodes_len = len(nodes_list)
        node_filename = nodes_list[1]
        jpg_filename = node_filename.text
        node_size = nodes_list[4]
        width = float(node_size.getchildren()[0].text)
        height = float(node_size.getchildren()[1].text)
        box_info = defaultdict(list)
        for i in range(6, nodes_len):
            node = nodes_list[i]
            label = node.getchildren()[0].text
            xmin = int(node.getchildren()[4].getchildren()[0].text)
            ymin = int(node.getchildren()[4].getchildren()[1].text)
            xmax = int(node.getchildren()[4].getchildren()[2].text)
            ymax = int(node.getchildren()[4].getchildren()[3].text)
            rect = (xmin,xmax,ymin,ymax)
            box_info[label].append(rect)
        return jpg_filename,(width,height),box_info



    def gen_example(self):
        anno_list, len_anno_list = self.get_xml_list()
        for i in tqdm(anno_list):
            xml_path = os.path.join(self.anno_path,i)
            if not os.path.exists(xml_path):
                continue
            jpg_filename,(width,height),box_info = self.read_xml(xml_path)
            img = cv2.imread(os.path.join(self.img_path,jpg_filename))
            #cv2.imshow("a",img)
            #cv2.waitKey()
            for key, value in box_info.items():
                for index, rect in enumerate(value):
                    xmin, xmax, ymin, ymax = rect
                    old_w = xmax - xmin
                    new_w = random.randint(self.obj_w[0], self.obj_w[1])
                    # 缩放比例 (new / old)
                    r = new_w / old_w
                    if not self.scaleup:  # only scale down, do not scale up (for better test mAP)
                        r = min(r, 1.0)
                    in_img = cv2.resize(img,(0,0),fx=r,fy=r,interpolation=cv2.INTER_LINEAR)
                    # cv2.imwrite("a.jpg",in_img)
                    # cv2.imshow("b",in_img)
                    # cv2.waitKey()
                    self.randomToObj(in_img,box_info,rect,r)




    def randomToObj(self,img,box_info,rect,r):
        x1 = int(rect[0]*r)
        y1 = int(rect[2]*r)
        shape = img.shape[:2]  # [height, width]
        for i in range(self.per_numOfObj):
            top = random.randint(0,150)
            bottom = random.randint(0,150)
            left =random.randint(0,150)
            right = random.randint(0,150)
            if y1 + self.new_shape[1] - top - bottom > shape[0] or x1 + self.new_shape[0] - left - right > shape[1]:
                continue
            new_img = img[y1:y1 + self.new_shape[1] - top - bottom, x1:x1 + self.new_shape[0] - left - right,:]
            bx1 = left
            bx2 =self.new_shape[0] - right
            by1 = top
            by2 = self.new_shape[1] - bottom
            #print(bx1,bx2,by1,by2)
            out_img = cv2.copyMakeBorder(new_img, top, bottom, left, right, cv2.BORDER_CONSTANT, value=self.color)
            #cv2.rectangle(out_img, (bx1, by1), (bx2, by2), (0, 255, 0), 2)

            cv2.imwrite(os.path.join(self.outImg_path, "{:0>6d}.jpg".format(self.k)), out_img)
            f = open(os.path.join(self.outAnno_path, "{:0>6d}.txt".format(self.k)), 'w')
            for key,value in box_info.items():
                for index, rect in enumerate(value):
                    # new_img = img[y1:y1+self.new_shape[1]-top-bottom,x1:x1+self.new_shape[0]-left-right]
                    newXmin = int(round((rect[0])*r-x1+left))
                    newYmin = int(round((rect[2])*r-y1+top))
                    newXmax = int(round((rect[1])*r-x1+left))
                    newYmax = int(round((rect[3])*r-y1+top))
                    cx = (newXmax-newXmin)/3*2
                    cy = (newYmax-newYmin)/3*2
                    if newXmax < bx1 or newXmin > bx2 or newYmax < by1 or newYmin > by2:
                        continue
                    if newXmax-bx1<cx or bx2-newXmin<cx or newYmax-by1<cy or by2-newYmin<cy:
                        continue
                    if key == '.':
                        label = 10
                    else:
                        label = key
                    #print(label)
                    #print("xmin:{}\tymin:{}\txman:{}\tymax:{}".format(newXmin,newYmin,newXmax,newXmax))
                    #cv2.rectangle(out_img,(newXmin,newYmin),(newXmax,newYmax),(0,0,255),2)

                    height,width = out_img.shape[:2] # [height, width]
                    yolo_x = (newXmin + (newXmax - newXmin) / 2.) / width
                    yolo_y = (newYmin + (newYmax - newYmin) / 2.) / height
                    yolo_w = (newXmax - newXmin) / width
                    yolo_h = (newYmax - newYmin) / height
                    f.write("{} {:.6f} {:.6f} {:.6f} {:.6f}\n".format(label, yolo_x, yolo_y, yolo_w, yolo_h))
            f.close()
            self.k+=1
            #print(self.k)


            # cv2.imshow("a",out_img)
            # cv2.waitKey()


if __name__ == '__main__':
    """
    从5000*3000的大图中选取目标，进行随机截取并且填充成640*640的样本作为训练集
    img_path = 图片绝对路径
    anno_path = 标注信息路径（xml）
    out_path = 图片以及标志信息输入的绝对路径
    其中标注信息格式为：  xxx.txt  yolov5格式
    """
    img_path = r'D:\practice\detection\Digit_recognition\data\img'
    anno_path = r'D:\practice\detection\Digit_recognition\data\anno\xml'
    out_path = r'D:\practice\detection\Digit_recognition\data\train'
    gen_example(img_path,anno_path,out_path)