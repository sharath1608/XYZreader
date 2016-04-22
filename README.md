# XYZreader

This project focuses on restoring a reading app to meet the rules and specifications of material design. Following major changes were performed.

1. Introduction of CoordinatorLayout to show the effects of pinning the toolbar.
2. Used appcompat and android design library to incorporate widgets.
3. SharedElement animation and propogate the image from activity to detail fragment.
4. Made image sizes uniform and  
5. Discarded random color selection and used palette as per material standards.
6. Added a custom view to provide a darker content scrim for expandedToolbar title (with light text color)

Known Bugs:

1. The transition of the shared element is a bit glitchy and doesn't happen occasionally. The issue is being looked at
2. The title of the collapsibleLayout does not center perfectly well. No solution in sight yet.
