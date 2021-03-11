# coding:utf-8
"""
@ author:kkksqj
@ date:2021/2/1
"""

import os
import random
import cv2
from tqdm import tqdm
import numpy as np
import shutil
import multiprocessing
import xml.etree.ElementTree as ET
import warnings

warnings.filterwarnings('ignore')
class gen_example():
    def __init__(self,bg_dir,dg_dir,anno_dir,out_dir):
        # 初始化
        self.bg_dir = bg_dir    #背景图目录
        self.dg_dir = dg_dir    #前景图目录
        self.anno_dir = anno_dir    #标注信息目录
        # self.outImg_dir = outImg_dir    #样本输出目录
        # self.outAnno_dir = outAnno_dir  #标注信息输出目录
        self.outImg_dir = os.path.join(out_dir,"images")    #样本输出目录
        self.outAnno_dir = os.path.join(out_dir,"labels")  #标注信息输出目录
        self.img_width = 640  # 生成的图片的宽
        self.img_height = 480  #生成的图片的高
        self.min_interval = 10    # 数字之间的最小间隔
        self.max_interval = 30    # 数字之间的最大间隔
        self.min_obj_width = 40       # 数字的框的最小宽，高按比例缩放
        self.max_obj_width = 100     # 数字的框的最大宽，高按比例缩放
        self.min_objs = 2           # 每张图含最小目标数量
        self.max_objs = 5          # 每张图含最多目标数量
        self.example_num_NO_point = 20    # 样本的数量(不带.)
        self.example_num_WITH_point = 30   # 样本的数量(带.)
        self.point_list = []
        self.check_before_run()

        # self._gen_example_with_no_point(self.example_num_NO_point)
        # self._gen_example_with_point(self.example_num_WITH_point)

    def check_before_run(self):
        if not os.path.exists(self.bg_dir):
            raise RuntimeError("dir:{} is not available".format(self.bg_dir))
        if not os.path.exists(self.dg_dir):
            raise RuntimeError("dir:{} is not available".format(self.dg_dir))
        if not os.path.exists(self.anno_dir):
            raise RuntimeError("dir:{} is not available".format(self.anno_dir))
        if not os.path.exists(self.outImg_dir):
            os.makedirs(self.outImg_dir)
            print("makedirs:{}".format(self.outImg_dir))
        if not os.path.exists(self.outAnno_dir):
            os.makedirs(self.outAnno_dir)
            print("makedirs:{}".format(self.outAnno_dir))
        if os.listdir(self.outImg_dir) or os.listdir(self.outAnno_dir):
            print("dir:\t{}\tand\t{}\nThe two folders must be empty!!".format(self.outImg_dir,self.outAnno_dir))
            exit()


        # if os.path.exists(self.outImg_dir):
        #     shutil.rmtree(self.outImg_dir)
        # os.makedirs(self.outImg_dir)
        # print("makedirs:{}".format(self.outImg_dir))
        # if os.path.exists(self.outAnno_dir):
        #     shutil.rmtree(self.outAnno_dir)
        # os.makedirs(self.outAnno_dir)
        # print("makedirs:{}".format(self.outAnno_dir))

    # 返回 背景，前景，标注 信息列表
    def info_lists(self):
        bg_list =[]
        dg_list = []
        anno_list = []
        for i in os.listdir(self.bg_dir):
            bg_list.append(os.path.join(self.bg_dir,i))
        for i in os.listdir(self.dg_dir):
            dg_list.append(os.path.join(self.dg_dir,i))
        for i in os.listdir(self.anno_dir):
            if i.endswith(".xml"):
                anno_list.append(os.path.join(self.anno_dir,i))
        return bg_list, dg_list, anno_list

    # 样本生成（不带小数点），example_nums:样本数目
    def _gen_example_with_no_point(self):
        bg_list, dg_list, anno_list = self.info_lists()
        bg_len = len(bg_list)
        dg_len = len(dg_list)
        anno_len = len(anno_list)
        k = 1
        for i in tqdm(range(self.example_num_NO_point)):
            bg_index = random.randint(0,bg_len-1)
            bg_img = cv2.imread(bg_list[bg_index])
            width_new = self.img_width
            height_new = self.img_height
            backimg = cv2.resize(bg_img, (int(width_new), int(height_new)))
            #backimg1 = backimg.copy()
            obj_nums = random.randint(self.min_objs,self.max_objs)
            obj_width = random.randint(self.min_obj_width,self.max_obj_width)
            center_x = random.randint(obj_width,obj_width+150)
            center_y = random.randint(obj_width*2,(self.img_height-obj_width*2))
            interval = random.randint(self.min_interval,self.max_interval)
            f = open(os.path.join(self.outAnno_dir, "{:0>6d}.txt".format(k)), 'a+')
            for j in range(obj_nums): # obj_nums
                anno_index = random.randint(0,anno_len-1)
                anno_xml = anno_list[anno_index]
                jpg_filename, label, (xmin, ymin, xmax, ymax), (width, height),_ = self.read_xml(anno_xml)
                self.point_list += _
                if not label.isdigit():
                    continue
                dg_img = cv2.imread(os.path.join(self.dg_dir,jpg_filename))
                obj = dg_img[ymin:ymax,xmin:xmax,:]
                # obj = cv2.resize(obj,(self.obj_width,int(self.obj_width*obj.shape[0]/obj.shape[1])))
                obj = cv2.resize(obj, (obj_width, int(obj_width * obj.shape[0] / obj.shape[1])))
                mask = 255 * np.ones(obj.shape, obj.dtype)

                new_xmin = int(center_x - obj.shape[1] / 2.)
                new_ymin = int(center_y - obj.shape[0] / 2.)
                new_xmax = int(center_x + obj.shape[1] / 2.)
                new_ymax = int(center_y + obj.shape[0] / 2.)
                yolo_x = (new_xmin + (new_xmax-new_xmin)/2.) / backimg.shape[1]
                yolo_y = (new_ymin + (new_ymax-new_ymin)/2.) / backimg.shape[0]
                yolo_w = (new_xmax - new_xmin) / float(backimg.shape[1])
                yolo_h = (new_ymax - new_ymin) / float(backimg.shape[0])

                center = (center_x, center_y)
                center_x = center_x + interval + obj.shape[1]
                # center_y += intetrval

                if new_xmax > width_new or new_ymax > height_new or new_xmin < 0 or new_ymin < 0:
                    continue

                # Seamlessly clone src into dst and put the results in output
                backimg = cv2.seamlessClone(obj, backimg, mask, center, cv2.MONOCHROME_TRANSFER)
                #backimg1 = cv2.seamlessClone(obj, backimg1, mask, center, cv2.MIXED_CLONE)


                # Write results
                # cv2.rectangle(backimg, (new_xmin, new_ymin), (new_xmax, new_ymax), (255,0,0), 2)
                # cv2.imshow("a",backimg)
                # cv2.waitKey()
                f.write("{} {:6f} {:6f} {:6f} {:6f}\n".format(label, yolo_x,yolo_y,yolo_w,yolo_h))

            # Write results
            cv2.imwrite(os.path.join(self.outImg_dir,"{:0>6d}.jpg".format(k)), backimg)
            #cv2.imwrite(os.path.join(self.outImg_dir,"{:0>6d}.png".format(k)), backimg1)
            f.close()
            k += 1




    # 样本生成(带小数点)，example_nums:样本数目
    def _gen_example_with_point(self):
        bg_list, dg_list, anno_list = self.info_lists()
        bg_len = len(bg_list)
        dg_len = len(dg_list)
        anno_len = len(anno_list)
        k = self.example_num_NO_point+1
        for i in tqdm(range(self.example_num_WITH_point)):
            bg_index = random.randint(0,bg_len-1)
            bg_img = cv2.imread(bg_list[bg_index])
            width_new = self.img_width
            height_new = self.img_height
            backimg = cv2.resize(bg_img, (int(width_new), int(height_new)))
            #backimg1 = backimg.copy()
            obj_nums = random.randint(self.min_objs,self.max_objs)
            obj_width = random.randint(self.min_obj_width,self.max_obj_width)
            center_x = random.randint(obj_width,obj_width+150)
            center_y = random.randint(obj_width*2,(self.img_height-obj_width*2))
            interval = random.randint(self.min_interval,self.max_interval)
            f = open(os.path.join(self.outAnno_dir, "{:0>6d}.txt".format(k)), 'a+')
            point_index = random.randint(2,3)
            for j in range(obj_nums): # obj_nums
                anno_index = random.randint(0,anno_len-1)
                anno_xml = anno_list[anno_index]
                jpg_filename, label, (xmin, ymin, xmax, ymax), (width, height),_points_list = self.read_xml(anno_xml)
                self.point_list += _points_list
                if not label.isdigit() and j+1!=point_index:
                    continue
                if j+1==point_index:
                    point_dict = random.choice(self.point_list)
                    jpg_filename = point_dict['name']
                    [xmin,ymin,xmax,ymax] = point_dict['rect']
                    label = 10
                dg_img = cv2.imread(os.path.join(self.dg_dir,jpg_filename))
                obj = dg_img[ymin:ymax,xmin:xmax,:]
                # obj = cv2.resize(obj,(self.obj_width,int(self.obj_width*obj.shape[0]/obj.shape[1])))
                obj = cv2.resize(obj, (obj_width, int(obj_width * obj.shape[0] / obj.shape[1])))
                mask = 255 * np.ones(obj.shape, obj.dtype)

                new_xmin = int(center_x - obj.shape[1] / 2.)
                new_ymin = int(center_y - obj.shape[0] / 2.)
                new_xmax = int(center_x + obj.shape[1] / 2.)
                new_ymax = int(center_y + obj.shape[0] / 2.)
                yolo_x = (new_xmin + (new_xmax-new_xmin)/2.) / backimg.shape[1]
                yolo_y = (new_ymin + (new_ymax-new_ymin)/2.) / backimg.shape[0]
                yolo_w = (new_xmax - new_xmin) / float(backimg.shape[1])
                yolo_h = (new_ymax - new_ymin) / float(backimg.shape[0])

                center = (center_x, center_y)
                center_x = center_x + interval + obj.shape[1]
                # center_y += intetrval

                if new_xmax > width_new or new_ymax > height_new or new_xmin < 0 or new_ymin < 0:
                    continue

                # Seamlessly clone src into dst and put the results in output
                backimg = cv2.seamlessClone(obj, backimg, mask, center, cv2.MONOCHROME_TRANSFER)
                #backimg1 = cv2.seamlessClone(obj, backimg1, mask, center, cv2.MIXED_CLONE)

                # Write results
                # cv2.rectangle(backimg, (new_xmin, new_ymin), (new_xmax, new_ymax), (255,0,0), 2)
                # cv2.imshow("a",backimg)
                # cv2.waitKey()
                f.write("{} {:6f} {:6f} {:6f} {:6f}\n".format(label, yolo_x,yolo_y,yolo_w,yolo_h))

            # Write results
            cv2.imwrite(os.path.join(self.outImg_dir,"{:0>6d}.jpg".format(k)), backimg)
            #cv2.imwrite(os.path.join(self.outImg_dir,"{:0>6d}.png".format(k)), backimg1)
            f.close()
            k += 1

    # 读取xml信息
    def read_xml(self,xml):
        tree = ET.parse(xml)
        root = tree.getroot()
        nodes_list = list(root)
        nodes_len = len(nodes_list)
        node_size = nodes_list[4]
        node_filename = nodes_list[1]
        xml_fiename = node_filename.text
        jpg_filename = xml_fiename[:-4]+".jpg"
        width = float(node_size.getchildren()[0].text)
        height = float(node_size.getchildren()[1].text)
        node_index = random.randint(6,nodes_len-1)
        node = nodes_list[node_index]
        label = node.getchildren()[0].text
        xmin = int(node.getchildren()[4].getchildren()[0].text)
        ymin = int(node.getchildren()[4].getchildren()[1].text)
        xmax = int(node.getchildren()[4].getchildren()[2].text)
        ymax = int(node.getchildren()[4].getchildren()[3].text)

        points_list = []
        points_dict = {}
        for i in range(6,nodes_len):
            p_node = nodes_list[i]
            p_label = p_node.getchildren()[0].text
            if p_label == '.':
                p_label = 10
                p_xmin = int(p_node.getchildren()[4].getchildren()[0].text)
                p_ymin = int(p_node.getchildren()[4].getchildren()[1].text)
                p_xmax = int(p_node.getchildren()[4].getchildren()[2].text)
                p_ymax = int(p_node.getchildren()[4].getchildren()[3].text)
                points_dict['name'] = jpg_filename
                points_dict['rect'] = [p_xmin,p_ymin,p_xmax,p_ymax]
                points_list.append(points_dict)
        return jpg_filename, label, (xmin,ymin,xmax,ymax),(width,height), points_list


if __name__ == '__main__':
    """
    脚本说明：
    1.训练样本生成脚本
    2.多线程

    操作说明：
    1.填写正确的目录
    2.在gen_example中的init中初始化参数
    3.运行脚本，生成样本
    注意：你要确保 outImg_dir，outAnno_dir 这两个输出目录为空，即不含任何文件！否则txt会出错！！！

    可以的操作的具体参数如下所示：

        self.img_width = 640            # 生成的图片的宽
        self.img_height = 480           # 生成的图片的高
        self.min_interval = 10          # 数字之间的最小间隔
        self.max_interval = 30          # 数字之间的最大间隔
        self.min_obj_width = 40         # 数字的框的最小宽，高按比例缩放
        self.max_obj_width = 80         # 数字的框的最大宽，高按比例缩放
        self.min_objs = 1               # 每张图含最小目标数量
        self.max_objs = 5              # 每张图含最多目标数量    
        self.example_num_NO_point = 20    # 样本的数量(不带.)
        self.example_num_WITH_point = 30   # 样本的数量(带.)   
    """


    #背景图目录
    bg_dir = r'D:\practice\detection\Digit_recognition\data\bg'
    #前景图目录
    dg_dir = r'D:\practice\detection\Digit_recognition\data\img'
    #xml标注信息目录
    anno_dir = r'D:\practice\detection\Digit_recognition\data\anno\xml'
    #结果的输出目录(自动在该目录下生成images文件夹(存放图片)和labels文件夹(存放标注txt))
    outdir = r'D:\practice\detection\Digit_recognition\digit'
    #gen_example(bg_dir,dg_dir,anno_dir,outImg_dir,outAnno_dir)

    process = gen_example(bg_dir,dg_dir,anno_dir,outdir)
    p1 = multiprocessing.Process(target=process._gen_example_with_no_point,
                                 args=())
    p2 = multiprocessing.Process(target=process._gen_example_with_point,
                                 args=())


    p1.start()
    p2.start()

