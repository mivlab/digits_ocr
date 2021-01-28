import cv2
import numpy as np
import os
import json
from tqdm import tqdm
from random import randint
import array

#用于创建路径 存在则不创建
def create_path(path):
    if os.path.exists(path):
        print('exist')
    else:
        os.mkdir(path)

#随机取一个数字 输入为存取数字的文件夹 返回取的数字图片和图片名称
def get_picture_dir(file_dir):
    filelist = os.listdir(file_dir)
    total_num=len(filelist)
    idx = randint(0,total_num-1)
    image_path=filelist[idx]
    imgae_name,_ = os.path.splitext(image_path)
    picture_path = os.path.join(os.path.abspath(file_dir), image_path)
    txt_path = imgae_name+".txt"
    #print(image_path)
    num=cv2.imread(picture_path)
    # cv2.imshow("img",num)
    # cv2.waitKey(0)
    return num,txt_path

#将已标注的图片中数字裁剪并保存于文件夹 输入已经标注好的图片文件夹地址root和对应txt地址txt_path  输出 数字和小数点图片与txt 数字小数点缩放至高100像素 宽等比例
def rect(txt_path, root, num,point,txtnum,txtpoint):
    for file in tqdm(os.listdir(txt_path)):
        file_name, ext= os.path.splitext(file)
        img = cv2.imread(os.path.join(root,file_name+'.jpg'))
        #print(img.shape)
        img_w=img.shape[0]
        img_h=img.shape[1]
        file1 = open(os.path.join(txt_path,file_name+'.txt'),'r')
        #print(file1.readlines())
        i=0
        for line in file1.readlines():
            i+=1
            curline = line.strip().split(" ")
            category=int(curline[0])
            x0 = float(curline[3])*img_h
            y0 = float(curline[4])*img_w
            x1 = float(curline[1])*img_h+0.5*x0
            y1 = float(curline[2])*img_w+0.5*y0
            x2 = float(curline[1])*img_h-0.5*x0
            y2 = float(curline[2])*img_w-0.5*y0
            numw=x1-x2
            numh=y1-y2
            im = img[int(y2):int(y1), int(x2):int(x1), :] # 裁剪坐标为[y0:y1, x0:x1]
            height_new = 100
            width_new = numw * height_new / numh
            img_new = cv2.resize(im, (int(width_new), height_new))

            if category==10:
                cv2.imwrite(os.path.join(point, file_name + str(i) + '.jpg'), img_new)
                with open(os.path.join(txtpoint, file_name + str(i) + '.txt'), 'a') as f1:
                    f1.writelines('%d %f %f \n' % (category, (width_new), (height_new)))
            else:
                cv2.imwrite(os.path.join(num, file_name + str(i) + '.jpg'), img_new)
                with open(os.path.join(txtnum, file_name + str(i) + '.txt'), 'a') as f1:
                    f1.writelines('%d %f %f \n' % (category, (width_new), (height_new)))

#遍历背景图随机抽取数字再任意位置混叠 输入backgnd背景图片 数字小数点及txt 输出为out最终图片 和对应txt
def mix(backgnd,num,point,out,outtxt,txtnum,txtpoint):

    for file in tqdm(os.listdir(backgnd)):
        file_name, ext= os.path.splitext(file)
        backimg = cv2.imread(os.path.join(backgnd,file_name+'.jpg'))
        ispoint = randint(0,6) #是否有小数点
        if ispoint >4:
            number = randint(2,4)
            height, width, channels = backimg.shape
            numimg = []
            num_path =[]
            numberx = []
            numbery = []
            cate = []
            for i in range(0,number,1):
                a, b = get_picture_dir(num)
                numimg.append(a)
                num_path.append(b)
                filenum = open(os.path.join(txtnum, str(num_path[i])), 'r')
                numberline = filenum.readlines()
                for lines in numberline:
                    curline = lines.strip().split(" ")
                cate.append(curline[0])
                numberx.append(curline[1])
                numbery.append(curline[2])

            muny=list(map(float,numbery))
            numy=max(muny)
            numy=int(numy)
            xbox = width - 50
            ybox = height - numy -20

            for i in range(0,number,1):
                xbox = xbox - int(float(numberx[i]))

            localx = randint(50,xbox)
            localy = randint(60,ybox)

            n=0;
            for i in range(0,number,1):
                center = (localx, localy)
                mask = 255 * np.ones(numimg[i].shape, numimg[i].dtype)
                backimg = cv2.seamlessClone(numimg[i], backimg, mask, center, cv2.MIXED_CLONE)
                newx = localx / width
                newy = localy / height
                numberwidth=int(float(numberx[i]))/width
                numberheight=int(float(numbery[i]))/height
                with open(os.path.join(outtxt, file_name + '.txt'), 'a') as f1:
                    f1.writelines('%d %f %f %f %f\n' % (int(cate[i]), (newx), (newy), (numberwidth), (numberheight)))
                #localx = localx +int(float(numberx[i]))
                if n < number -1:
                    n=i+1
                else:
                    n = i
                localx = localx +  int(0.5*float(numberx[i]))+int(0.5*float(numberx[n]))
                cv2.imwrite(os.path.join(out, file_name + '.jpg'), backimg)

        else:
            number = randint(1,3)
            height, width, channels = backimg.shape
            numimg = []
            num_path = []
            numberx = []
            numbery = []
            cate = []
            for i in range(0, number, 1):
                a, b = get_picture_dir(num)
                numimg.append(a)
                num_path.append(b)
                filenum = open(os.path.join(txtnum, str(num_path[i])), 'r')
                numberline = filenum.readlines()
                for lines in numberline:
                    curline = lines.strip().split(" ")
                cate.append(curline[0])
                numberx.append(curline[1])
                numbery.append(curline[2])

            a, b = get_picture_dir(point)
            numimg.append(a)
            num_path.append(b)
            filenum = open(os.path.join(txtpoint, str(num_path[number])), 'r')
            numberline = filenum.readlines()
            for lines in numberline:
                curline = lines.strip().split(" ")
            cate.append(curline[0])
            numberx.append(curline[1])
            numbery.append(curline[2])

            number2 = randint(1, 3)
            for n in range(0, number2, 1):
                a, b = get_picture_dir(num)
                numimg.append(a)
                num_path.append(b)
                count=number+n+1
                filenum = open(os.path.join(txtnum, str(num_path[count])), 'r')
                numberline = filenum.readlines()
                for lines in numberline:
                    curline = lines.strip().split(" ")
                cate.append(curline[0])
                numberx.append(curline[1])
                numbery.append(curline[2])


            muny = list(map(float, numbery))
            numy = max(muny)
            numy = int(numy)
            xbox = width - 50
            ybox = int(height - 0.7*numy - 20)

            for i in range(0, count, 1):
                xbox = xbox - int(float(numberx[i]))

            localx = randint(50, xbox)
            localy = randint(60 , ybox)
            n = 0
            count+=1
            for i in range(0, count, 1):
                center = (localx, localy)
                mask = 255 * np.ones(numimg[i].shape, numimg[i].dtype)
                backimg = cv2.seamlessClone(numimg[i], backimg, mask, center,cv2.NORMAL_CLONE) #cv2.NORMAL_CLONE cv2.MONOCHROME_TRANSFER cv2.MONOCHROME_TRANSFER 泊松混叠可用参数 其余混叠方式 还在尝试
                newx = localx / width
                newy = localy / height
                numberwidth = int(float(numberx[i])) / width
                numberheight = int(float(numbery[i])) / height
                with open(os.path.join(outtxt, file_name + '.txt'), 'a') as f1:
                    f1.writelines('%d %f %f %f %f\n' % (int(cate[i]), (newx), (newy), (numberwidth), (numberheight)))

                if n < count -1:
                    n=i+1
                else:
                    n = i

                localx = localx + int(0.5*float(numberx[n]))+ int(0.5*float(numberx[i]))
                cv2.imwrite(os.path.join(out, file_name + '.jpg'), backimg)


if __name__ == '__main__':

    backgnd=r'E:\Data\python\1\demo\backgnd'
    # 图片路径
    root = r'E:\Data\python\1\demo\demo'
    # 存放txt的路径
    txt_path = r'E:\Data\python\1\demo\txt'
    #单个数字的txt
    txtnum=r'E:\Data\python\1\demo\txtnumber'
    #小数点txt
    txtpoint=r'E:\Data\python\1\demo\txtpoint'
    # 生成数字图像的路径
    num = r'E:\Data\python\1\demo\num'
    point=r'E:\Data\python\1\demo\point'
    #输出图片地址
    out = r'E:\Data\python\1\demo\out'
    outtxt = r'E:\Data\python\1\demo\outtxt'

    create_path(outtxt)


    rect(txt_path, root, num,point,txtnum,txtpoint)

    #mix(backgnd,num,point,out,outtxt,txtnum,txtpoint)