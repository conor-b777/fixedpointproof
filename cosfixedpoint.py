from math import *
from time import *

#gimme numbers aaaagh
n = int(input("\nEnter any number: "))
print()

#so theres no minus sign
n=cos(n)

#cosines the number
while True:
    n = cos(n)
    sleep(0.045)

#if its 0.73908513321516056127 then its at the fixed point
    if n == cos(n):
        print("\n\nFixed Point Reached!\n")
        break

#prints with decimal limit of 25
    else:
        print(f"cos({n:.25f})", end="\r", flush=True)
