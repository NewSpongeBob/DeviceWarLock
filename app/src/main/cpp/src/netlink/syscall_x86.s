.text
.global raw_syscall
.type raw_syscall,@function

raw_syscall:
    push    %ebp
    mov     %esp, %ebp
    push    %ebx          # 保存ebx
    push    %esi
    push    %edi

    mov     8(%ebp), %eax  # 系统调用号
    mov     12(%ebp), %ebx # 第一个参数
    mov     16(%ebp), %ecx # 第二个参数
    mov     20(%ebp), %edx # 第三个参数
    mov     24(%ebp), %esi # 第四个参数
    mov     28(%ebp), %edi # 第五个参数

    int     $0x80         # 执行系统调用

    pop     %edi
    pop     %esi
    pop     %ebx
    mov     %ebp, %esp
    pop     %ebp
    ret