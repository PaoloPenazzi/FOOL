push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function1
lhp
sw
lhp
push 1
add
shp
push 3
push -1
lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 10000
push -2
add
lhp
sw
lhp
push 1
add
push 5
print
halt

function0:
cfp
lra
lfp
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

function1:
cfp
lra
lfp
push -2
add
lw
stm
sra
pop
sfp
ltm
lra
js