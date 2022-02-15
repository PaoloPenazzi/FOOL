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
lhp
push function2
lhp
sw
lhp
push 1
add
shp
push function3
lhp
sw
lhp
push 1
add
shp
push 2
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
push 9998
lw
lhp
sw
lhp
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
push 9997
lw
lhp
sw
lhp
lhp
push 1
add
shp
push 1
push function4
lfp
lfp
push -5
add
lw
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
lw
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
lw
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

function2:
cfp
lra
lfp
lw
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

function3:
cfp
lra
lfp
lw
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

function4:
cfp
lra
lfp
lw
push -6
add
lw
stm
sra
pop
sfp
ltm
lra
js