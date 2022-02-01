push 0
push 5
push 5
push 2
add
sub
push 0
bleq label0
push 0
b label1
label0:
push 1
label1:
lfp
push -2
add
lw
push 1
sum
push 2
beq label4
push 0
b label5
label4:
push 1
label5:
push 1
beq label2
push 1
push 1
sub
b label3
label2:
push 1
label3:
print
halt