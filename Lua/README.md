# 

>Lua 是一种轻量小巧的脚本语言，用标准C语言编写并以源代码形式开放， 其设计目的是为了嵌入应用程序中，从而为应用程序提供灵活的扩展和定制功能。
>
>
>
>Redis的单个命令都是原子性的，有时候我们希望能够组合多个Redis命令，并让这个组合也能够原子性的执行，甚至可以重复使用，在2.6版本中引入了一个特性来解决这个问题，这就是Redis执行Lua脚本。
>
>
>
>Nginx也支持Lua，利用Lua也可以实现很多有用的功能。



## 基本语法

### 单行注释

```lua
--
```



### 多行数值

```lua
--[[
 多行注释
 多行注释
--]]
```



### 变量

```lua
-- 全局变量
name = "global name"

-- 局部变量
local age = 18
```





## 数据类型

Lua总共有8种类型，但对于脚本使用来说，只需要5种类型即可

1. `nil` 空
2. `boolean` 布尔值
3. `number` 数字
4. `string` 字符串
5. `table` 表



### nil (空)

- 未声明的变量输出会直接报nil

  ```lua
  > print(a)
  nil
  >
  ```

  

- 需要删除变量(如数组中的元素)，直接将变量赋值为nil即可

  ```lua
  tab1 = { key1 = "val1", key2 = "val2", "val3" }
  for k, v in pairs(tab1) do
      print(k .. " - " .. v)
  end
  
  -- 变量赋值为nil
  tab1.key1 = nil
  for k, v in pairs(tab1) do
      print(k .. " - " .. v)
  end
  ```

  输出结果

  ```
  1 - val3
  key1 - val1
  key2 - val2
  ------------
  1 - val3
  key2 - val2
  ```

  

- 需要比较是否为空的时候，nil需要加上双引号

  ```lua
  > type(a)   
  nil
  > type(a) == nil
  false
  > type(a) == "nil" 
  true
  ```



### boolean (布尔)

- lua中，只有false和nil为假，其他都为真

  ```lua
  if false or nil then
      print("至少有一个是 true")
  else
      print("false 和 nil 都为 false")
  end
  
  if 0 then
      print("数字 0 是 true")
  else
      print("数字 0 为 false")
  end
  ```

  输出结果

  ```bash
  false 和 nil 都为 false
  数字 0 是 true
  ```



### number (数字)

- lua中数字只有double(双精度类型)



### string (字符串)

- 字符串可以用单引号或双引号标识

  ```lua
  str1 = "str1"
  str2 = 'str2'
  ```

  

- 多行字符串可以用2个方括号 `[[]]` 表示

  ```lua
  str = [[
  hello
  world
  ]]
  print(str)
  ```

  执行结果

  ```bash
  hello
  world
  ```

  

- 在对一个数字字符串上进行算术操作时，Lua 会尝试将这个数字字符串转成一个数字

  ```lua
  -- https://www.w3cschool.cn/lua/lua-data-types.html
  
  > print("2" + 6)
  8.0
  > print("2" + "6")
  8.0
  > print("2 + 6")
  2 + 6
  > print("-2e2" * "6")
  -1200.0
  > print("error" + 1)
  stdin:1: attempt to perform arithmetic on a string value
  stack traceback:
   stdin:1: in main chunk
      [C]: in ?
  > 
  ```

   

- 字符串链接用  `..`  (数字类使用..也会转化为字符串) 

  ```lua
  > print("abc" .. "def")
  abcdef
  > print(123 .. 456)
  123456
  ```

  
  
- 使用  `#`  来计算字符串的长度

  ```lua
  > print(#'abc')
  3 
  > print(#('abc' .. 'def'))
  6 
  ```



### table (数组，字典)

- 普通的数组

  ```lua
  -- 创建一个空的 table
  local table1 = {}
  
  -- 直接初始表(此处没有赋值key，则key默认是下标，且下标从1开始)
  local table2 = {"apple", "pear", "orange", "grape"}
  
  -- 输出表大小
  print(#table1)  -- 0
  print(#table2)  -- 4
  ```

  遍历数组

  ``` lua
  for k, v in pairs(table2) do
      print(k .. " : " .. v)
  end
  ```

  输出结果(key是下标)

  ```bash
  1 : apple
  2 : pear
  3 : orange
  4 : grape
  ```

  
  
- 字典

  ```lua
  local map = { name = "mike", age = 18 }
  -- 输出长度(作为字典时，lua的长度不准确)
  print(#map)
  -- 输出name, 注意需要引号
  print(map['name'])
  -- 输出age
  print(map.age)
  ```

  输出结果

  ```bash
  0
  mike
  18
  ```

  







## 参考资料

- [Lua 教程_w3cschool](https://www.w3cschool.cn/lua/)
- [Redis Lua脚本完全入门 - 码农小胖哥 - 博客园 (cnblogs.com)](https://www.cnblogs.com/felordcn/p/13838321.html)



