push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push 3
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
shp
lfp
lfp
stm
ltm
ltm
lw
push 0
add
lw
js
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