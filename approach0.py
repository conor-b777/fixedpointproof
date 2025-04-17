from math import *
from time import *

#gimme numbers aaaagh
n = int(input("\nEnter any number: "))
print()

#halves the number
while True:
    n = n/2
    sleep(0.045)

    print({n}, end="\r")
