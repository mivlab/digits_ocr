# coding:utf-8
"""

@ author: kkksqj
@ date: 2021/3/8
"""
import os
import warnings
import xml.etree.ElementTree as ET
import random

from tqdm import tqdm

warnings.filterwarnings('ignore')



"""
drawParm: 通过xml文件读取屏幕以及roi参数，由程序自动转换出安卓程序屏幕以及roi相对位置参数

param:   f: 记录结果的txt文件，已经通过open打开
         xml:xml文件的绝对路径  (由labelimg根据截取屏幕以及roi区域)
         xPos:手工设置的屏幕x初始位置(相对画布的百分比)
         wPos:手工设置的屏幕宽度(相对画布的百分比)
         
输出：    box:[xpos, ypos wpos,hpos]
         roi:[inX, inY, inW, inH]
"""
def drawParam(f,xml,xPos,wPos):
    tree = ET.parse(xml)
    root = tree.getroot()
    nodes_list = list(root)
    nodes_len = len(nodes_list)
    node_filename = nodes_list[1]
    xml_fiename = node_filename.text
    f.write("file name: " + xml_fiename+'\n')
    pic_size = nodes_list[4]
    # 图片尺寸
    width = float(pic_size.getchildren()[0].text)
    height = float(pic_size.getchildren()[1].text)

    # 屏幕尺寸
    box_size = nodes_list[6]
    xmin = int(box_size.getchildren()[4].getchildren()[0].text)
    ymin = int(box_size.getchildren()[4].getchildren()[1].text)
    xmax = int(box_size.getchildren()[4].getchildren()[2].text)
    ymax = int(box_size.getchildren()[4].getchildren()[3].text)
    box_width = xmax-xmin
    box_height = ymax-ymin
    box_scale = box_height/box_width
    hPos = box_scale/0.75*wPos
    yPos = (1-hPos)/2.
    f.write("box location:\n[x y w h] = [{:.3f} {:.3f} {:.3f} {:.3f}]\n".format(xPos,yPos,wPos,hPos))

    # roi尺寸
    roiLocate = []
    k = 0
    f.write("roi location:\n")
    for i in range(7, nodes_len):
        node = nodes_list[i]
        # label = node.getchildren()[0].text
        roi_xmin = int(node.getchildren()[4].getchildren()[0].text)
        roi_ymin = int(node.getchildren()[4].getchildren()[1].text)
        roi_xmax = int(node.getchildren()[4].getchildren()[2].text)
        roi_ymax = int(node.getchildren()[4].getchildren()[3].text)

        a = (roi_xmin-xmin)/box_width
        b = (roi_ymin-ymin)/box_height
        c = (roi_xmax-xmin)/box_width
        d = (roi_ymax-ymin)/box_height

        locateX = a*wPos+xPos
        locateY = b*hPos+yPos
        locateW = c*wPos+xPos-locateX
        locateH = d*hPos+yPos-locateY

        f.write("[x y w h] = [{:.3f} {:.3f} {:.3f} {:.3f}]\n".format(locateX,locateY,locateW,locateH))
        roiLocate.append((locateX,locateY,locateW,locateH))
        k+=1
    f.write("roi nums:"+str(k)+"\n")
    f.write("\n")
    return (xPos,yPos,wPos,hPos),roiLocate,k

"""
获取结果txt, 调用drawParam
"""
def getLocation(xmlPath):
    if not os.path.exists(xmlPath):
        print("ERROR : path of xml not available.\nxml path:{}".format(xmlPath))
        exit()
    f = open(os.path.join(xmlPath,"result.txt"),'w')
    for xml in tqdm(os.listdir(xmlPath)):
        if xml.endswith(".xml"):
            file = os.path.join(xmlPath, xml)
            (xPos, yPos, wPos, hPos), roiLocate, k = drawParam(f,file,0.15,0.7)
    f.close()


######################################

"""
没有txt写入，可以支持单个xml测试
"""
def drawParamOne(xml,xPos,wPos):
    tree = ET.parse(xml)
    root = tree.getroot()
    nodes_list = list(root)
    nodes_len = len(nodes_list)
    node_filename = nodes_list[1]
    xml_fiename = node_filename.text
    pic_size = nodes_list[4]
    # 图片尺寸
    width = float(pic_size.getchildren()[0].text)
    height = float(pic_size.getchildren()[1].text)

    # 屏幕尺寸
    box_size = nodes_list[6]
    xmin = int(box_size.getchildren()[4].getchildren()[0].text)
    ymin = int(box_size.getchildren()[4].getchildren()[1].text)
    xmax = int(box_size.getchildren()[4].getchildren()[2].text)
    ymax = int(box_size.getchildren()[4].getchildren()[3].text)
    box_width = xmax-xmin
    box_height = ymax-ymin
    box_scale = box_height/box_width
    hPos = box_scale/0.75*wPos
    yPos = (1-hPos)/2.


    # roi尺寸
    roiLocate = []
    k = 0
    for i in range(7, nodes_len):
        node = nodes_list[i]
        # label = node.getchildren()[0].text
        roi_xmin = int(node.getchildren()[4].getchildren()[0].text)
        roi_ymin = int(node.getchildren()[4].getchildren()[1].text)
        roi_xmax = int(node.getchildren()[4].getchildren()[2].text)
        roi_ymax = int(node.getchildren()[4].getchildren()[3].text)

        a = (roi_xmin-xmin)/box_width
        b = (roi_ymin-ymin)/box_height
        c = (roi_xmax-xmin)/box_width
        d = (roi_ymax-ymin)/box_height

        locateX = a*wPos+xPos
        locateY = b*hPos+yPos
        locateW = c*wPos+xPos-locateX
        locateH = d*hPos+yPos-locateY

        roiLocate.append((locateX,locateY,locateW,locateH))
        k+=1
    return (xPos,yPos,wPos,hPos),roiLocate,k

"""
支持单个xml测试，调用函数drawParamOne(xml,xPos,wPos)
"""
def getLocationOne():
    xml = r'C:\Users\Administrator\Desktop\img\9.xml'
    xpos = 0.2
    wpos = 0.6
    f = open("result.txt")
    (xPos,yPos,wPos,hPos),roiLocate,k = drawParamOne(xml,xpos,wpos)
    p = (xPos,yPos,wPos,hPos)
    print(k)
    print(p)
    print(roiLocate)

#######################################
"""
文件批量重命名
"""
def rename(path):
    fileList = os.listdir(path)
    fileLen = len(fileList)
    random.shuffle(fileList)
    print("file nums: "+ str(fileLen))
    for index, file in enumerate(fileList):
        oldName = fileList[index]
        newName = "{:0>6d}.jpg".format(index)
        os.rename(os.path.join(path,oldName),os.path.join(path,newName))
        print(oldName, "=====>" ,newName)



if __name__ == '__main__':

    # 从xml中批量获取xywh的相对坐标，用于安卓程序
    # xmlPath = r'C:\Users\Administrator\Desktop\img'
    # getLocation(xmlPath)

    # 文件批量重命名
    path = r'D:\practice\detection\Digit_recognition\data\DataFromPad'
    rename(path)

