# 项目基本功能介绍

- ###### 用户注册：实现了数据校验、账号查重、数据加密、数据插入

![image-20250220170206656](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220170206656.png)

- ###### 用户登录：采用了基于 Session 的登录态管理实现用户登录；进行参数校验、密码比对、Session 存储、返回脱敏数据

![image-20250220170449001](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220170449001.png)

- ###### 权限管理：用户和管理员

1、管理员界面：

![image-20250220170858680](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220170858680.png)

2、用户界面：

![image-20250220170934829](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220170934829.png)

3、普通用户无法访问管理员页面

![image-20250220172914446](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220172914446.png)

- ###### 图片上传（公共图库）功能：支持从本地或通过 URL 上传图片

![image-20250220171324705](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220171324705.png)

![image-20250220171813294](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220171813294.png)

- ###### 图片编辑页有图片编辑（裁剪、旋转）功能、AI 扩图功能

编辑功能

![image-20250220191251348](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220191251348.png)

**AI 扩图功能**

![image-20250220191337736](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220191337736.png)

![image-20250220191414205](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220191414205.png)

![image-20250220191424439](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220191424439.png)

- ###### 普通用户上传的图片需要由管理员审核才会展示在公共图库

1) 管理员图片管理（审核）界面

![image-20250220172128329](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220172128329.png)

2) 审核通过后我们便能够在公共图库看到刚才的图片

![image-20250220172215711](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220172215711.png)

3) 点击就可以查看图片详情页面

![image-20250220172756685](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220172756685.png)

- ###### 图片支持下载、分享、编辑和删除（编辑和删除仅管理员和本人拥有权限）

分享实现了链接分享和二维码分享

![image-20250220180156879](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220180156879.png)

- ###### 管理员支持用户管理、图片管理和空间管理

1、用户管理、支持按照账号或者昵称进行搜索

![image-20250220183331251](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220183331251.png)

![image-20250220183823180](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220183823180.png)

2、图片管理功能：采用分页技术、支持多类型查询、**支持批量创建图片**

![image-20250220184032010](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220184032010.png)

创建成功后自动跳转首页，并将图片展示到公共图库

![image-20250220184059493](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220184059493.png)

![image-20250220184238906](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220184238906.png)

3、空间管理：也是支持多类型查询、增加了分析空间的功能、便于管理员对空间资源占用进行分析

![image-20250220184511274](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220184511274.png)

**实现了存储空间、图片数量的占用比分析；图片分类分析；图片标签词云展示；空间图片大小占比分析；用户上传图片情况分析；空间使用排行分析。可以对公共空间和全空间进行分析。**

![image-20250220184730789](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220184730789.png)

- ###### 个人空间：用于不想展示在公共图库的照片，可用作个人图库，添加了更多类型的搜索词条；具有空间分析功能，增加了批量编辑功能

管理员goat的个人空间

![image-20250220185235424](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220185235424.png)

普通用户user002的个人空间（区别有普通版、专业版、旗舰版，对应的容量和可上传数量不同）

![image-20250220190603365](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220190603365.png)

进行批量编辑（只对当前页面图片进行编辑）

![image-20250220190802150](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220190802150.png)

![image-20250220190858041](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220190858041.png)

![image-20250220190916117](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220190916117.png)

用户个人空间分析（删除了用户上传情况分析、空间占用排行分析）

![image-20250220190944376](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220190944376.png)

- ###### 团队空间：拥有者可以邀请成员加入、更改成员权限、进行实时协同编辑、进行空间分析。

**以 user002 开通的团队空间为例**

团队空间首页

![image-20250220192136979](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220192136979.png)

进行成员管理并邀请成员加入

![image-20250220192019334](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220192019334.png)

![image-20250220192306510](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220192306510.png)

实施协同编辑

![image-20250220192437260](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220192437260.png)

![image-20250220192528727](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20250220192528727.png)