# coding:utf-8
"""
@ author:kkksqj
@ date:2021/2/4
"""

import os
import cv2
import glob
import random
from tqdm import tqdm
import argparse
import sys
def train_val_split(path,ratio):
    train_txt_path = os.path.join(path,"train.txt")
    val_txt_path = os.path.join(path,"val.txt")

    # anno_txt_list = glob.glob(txt_path+'/*.txt')
    anno_txt_list = os.listdir(os.path.join(path,"labels"))
    random.shuffle(anno_txt_list)
    num = len(anno_txt_list)
    train_list = anno_txt_list[:int(ratio*num)]
    val_list = anno_txt_list[int(ratio*num):]
    with open(train_txt_path,'w') as f:
        for line in tqdm(train_list):
            jpg_name = line.replace('txt','jpg')
            jpg_path = os.path.join(path,"images",jpg_name)
            img = cv2.imread(jpg_path)
            if img is not None:
                f.write(jpg_path+'\n')

    with open(val_txt_path,'w') as f:
        for line in tqdm(val_list):
            jpg_name = line.replace('txt','jpg')
            jpg_path = os.path.join(path,"images",jpg_name)
            img = cv2.imread(jpg_path)
            if img is not None:
                f.write(jpg_path+'\n')

    print("训练集数量：{}\n验证集数量：{}".format(len(train_list),len(val_list)))



if __name__ == '__main__':
    """
    脚本说明：训练集和验证集划分，并且在path目录下生成train.txt和val.txt
    
    使用说明：
    参数1： path   # images和labels文件夹的根目录，如 path/images   path/labels
    参数2： ratio  # 训练集比验证集的比例，默认0.8
    """
    if len(sys.argv) > 1:
        parser = argparse.ArgumentParser()
        parser.add_argument("-p", "--path", type=str, help="图片根目录", required=True)
        parser.add_argument("-r", "--ratio",type=float, help="训练集比验证集的比例", default=0.8)
        args = parser.parse_args()
        train_val_split(args.path, args.ratio)
    else:
        # path:images和labels文件夹的根目录
        path = r'D:\practice\detection\Digit_recognition\digit'
        ratio = 0.8
        train_val_split(path,ratio)