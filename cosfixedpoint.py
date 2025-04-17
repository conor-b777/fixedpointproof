from math import *
from time import *

n = int(input("\nEnter any number: "))
print()

while True:
    n = cos(n)
    sleep(0.045)

    if n == cos(n):
        print("\n\nFixed Point Reached!\n")
        break

    else:
        # Format 'n' to 10 decimal places for consistent line length
        print(f"cos({n:.20f})", end='\r')