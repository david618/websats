# Create Ellipses

Goal: Create some test polygons (ellipses) for testing.

- Small ellipses 0.1 to 1km in size
- Random points over land areas
- Rotate the ellipse 
- Return as json,txt, or geojson to support loading via GeoEvent 
- Should also support loading centers as points

## Create Landgrids

The landgrids are a set of lower left lon,lat values representing grids where the area of the grid intersects land
Using Python created 1 degree grids for world 


App select random point based on set of "landgrids". 
Download 110m json for country boundaries from openlayers 
https://github.com/openlayers/openlayers/blob/master/examples/data/geojson/countries-110m.geojson 
Dataset I used excluded Antartica
Used QGIS to find intersection of grids with countries; spatial query faster if you convert geojson to Shapefiles and add index to each
Save the intersection to CSV with just of Lower Left Lon,Lat for each grid that intersects Land
The "ellipse" app selects a random point in one of the landgrids; then returns GeoJson of a random ellipse(s) with a and b values from 10 meters to 1010 meters.
https://esri105.westus.cloudapp.azure.com/websats/ellipses?f=geojson&num=10
For geojson set f=geojson
Set num to the desired number of ellipse you want the code to create
The number of points for each ellipse varies depending on the eccentricity of the ellipse; the code adds more dense points where the curve of the ellipse is most pronounced. 
The ellipses are rotated by a random angle. 


