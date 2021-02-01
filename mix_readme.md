将数字混叠至背景图片中 mix.py

txt标注文件均指图片对应的yolo格式标注

rect函数将已标注的图片中数字取出 

输入图片路径root和对应txt路径txt_path

输出单个数字与小数点jpg及其图片 类别，宽，高 存放在对应txt

mix函数输入背景图片backgnd 数字num 小数点point 和上述对应txt 的路径 

输出图片 和标注txt