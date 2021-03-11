# coding:utf-8
"""
@ author:kkksqj
@ date:2021/1/24
"""

import xml.etree.ElementTree as ET
import cv2
import os
from tqdm import tqdm
import argparse
import sys
import warnings

warnings.filterwarnings('ignore')
def xml_to_yolo(img_root,xml_root,txt_root,show = False):
    if not os.path.exists(xml_root):
        print("xml root:{} is not available".format(xml_root))
        exit()
    if not os.path.exists(img_root):
        print("img root:{} is not available".format(img_root))
        exit()
    if not os.path.exists(txt_root):
        os.makedirs(txt_root)
        print("mkdir:"+ txt_root)

    xml_list =[]
    dir = os.listdir(xml_root)
    with open(os.path.join(txt_root,"classes.txt"),'w') as f1:
        for i in range(10):
            f1.write("{}\n".format(i))
        f1.write("{}\n".format('.'))
    for xml in dir:
        if xml.endswith('.xml'):
            xml_list.append(xml)
    for xml in tqdm(xml_list):
        xml_ = os.path.join(xml_root,xml)
        tree = ET.parse(xml_)
        root = tree.getroot()
        nodes_list = list(root)
        nodes_len = len(nodes_list)
        node_filename = nodes_list[1]
        xml_fiename = node_filename.text
        txt_fiename = xml_fiename[:-4]+".txt"
        jpg_filename = xml_fiename[:-4]+".jpg"
        if show:
            img_root1 = os.path.join(img_root,jpg_filename)
            img = cv2.imread(img_root1)
        f = open(os.path.join(txt_root, txt_fiename),'w')
        node_size = nodes_list[4]
        width = float(node_size.getchildren()[0].text)
        height = float(node_size.getchildren()[1].text)
        for i in range(6,nodes_len):
            node = nodes_list[i]
            label = node.getchildren()[0].text
            xmin = int(node.getchildren()[4].getchildren()[0].text)
            ymin = int(node.getchildren()[4].getchildren()[1].text)
            xmax = int(node.getchildren()[4].getchildren()[2].text)
            ymax = int(node.getchildren()[4].getchildren()[3].text)
            if show:
                cv2.rectangle(img, (xmin, ymin), (xmax, ymax), (255,0,0), 2)
            if label == '.':
                label_index = 10
            elif (str(label).isdigit()==False):
                continue
            else:
                label_index = label
            yolo_x = (xmin+(xmax-xmin)/2.)/width
            yolo_y = (ymin+(ymax-ymin)/2.)/height
            yolo_w = (xmax-xmin)/width
            yolo_h = (ymax-ymin)/height
            f.write("{} {:.6f} {:.6f} {:.6f} {:.6f}\n".format(label_index,yolo_x,yolo_y,yolo_w,yolo_h))
        f.close()
        if show:
            img1 = cv2.resize(img, (int(width / 4), int(height / 4)))
            cv2.imshow("img",img1)
            cv2.waitKey()


if __name__ == '__main__':
    """
    脚本说明：
    使用labelimg生成的xml 转 yolo格式的txt
    
    yolo格式txt: label x y w h
    x = box的中心点box_x/图片宽
    y = box的中心点box_y/图片高
    w = box的宽box_w/图片宽
    h = box的高box_h/图片高
    
    参数说明：
    1.img_root:图片绝对路径
    2.xml_root: xml绝对路径
    3.txt_root: txt绝对路径(用于存放生成的txt)
    4.show:是否可视化图片 True:显示图片 False:不显示图片
    """
    if len(sys.argv) > 1:
        parser = argparse.ArgumentParser()
        parser.add_argument("-ir", "--img_root", type=str, help="图片绝对路径", required=True)
        parser.add_argument("-xr", "--xml_root",type=str, help="xml绝对路径", required=True)
        parser.add_argument("-tr", "--txt_root", type=str, help="txt绝对路径", required=True)
        parser.add_argument("-s", "--show",type=bool, help="是否展示图片", default=False)
        args = parser.parse_args()
        xml_to_yolo(args.img_root,args.xml_root,args.txt_root,args.show)
    else:
        # img_root: 图片绝对路径
        img_root = r'D:\practice\detection\Digit_recognition\data\img'
        # xml_root: xml绝对路径
        xml_root = r'D:\practice\detection\Digit_recognition\data\anno\xml'
        # txt_root: txt绝对路径(用于存放生成的txt)
        txt_root = r'D:\practice\detection\Digit_recognition\data\anno\txt'
        # show:是否可视化图片 True:显示图片 False:不显示图片
        show = False
        xml_to_yolo(img_root,xml_root,txt_root,show)
