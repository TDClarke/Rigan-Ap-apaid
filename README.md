In summary, the nudity detection algorithm works in the
following manner:

1. Scan the image starting from the upper left corner
to the lower right corner.

2. For each pixel, obtain the RGB component values.

3. Calculate the corresponding Normalized RGB and
HSV values from the RGB values.

4. Determine if the pixel color satisfies the
parameters for being skin established by the skin
color distribution model.

5. Label each pixel as skin or non-skin.

6. Calculate the percentage of skin pixels relative to
the size of the image.

7. Identify connected skin pixels to form skin regions.

8. Count the number of skin regions.

9. Identify pixels belonging to the three largest skin
regions.

10. Calculate the percentage of the largest skin region
relative to the image size.

11. Identify the leftmost, the uppermost, the rightmost,
and the lowermost skin pixels of the three largest
skin regions. Use these points as the corner points
of a bounding polygon.

12. Calculate the area of the bounding polygon.

13. Count the number of skin pixels within the
bounding polygon.

14. Calculate the percentage of the skin pixels within
the bounding polygon relative to the area of the
polygon.

15. Calculate the average intensity of the pixels inside
the bounding polygon.

16. Classify an image as follows:
a. If the percentage of skin pixels relative to
the image size is less than 15 percent, the
image is not nude. Otherwise, go to the
next step.

b. If the number of skin pixels in the largest
skin region is less than 35% of the total
skin count, the number of skin pixels in
the second largest region is less than
30% of the total skin count and the
number of skin pixels in the third largest
region is less than 30 % of the total skin
count, the image is not nude.

c. If the number of skin pixels in the largest
skin region is less than 45% of the total
skin count, the image is not nude.

d. If the total skin count is less than 30% of
the total number of pixels in the image
and the number of skin pixels within the
bounding polygon is less than 55 percent
of the size of the polygon, the image is
not nude.

e. If the number of skin regions is more
than 60 and the average intensity within
the polygon is less than 0.25, the image
is not nude.

f. Otherwise, the image is nude.
