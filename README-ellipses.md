# Create Ellipses

Goal: Create some test polygons (ellipses) for testing.

- Small ellipses 0.1 to 1km in size
- Random points over land areas
- Rotate the ellipse 
- Return as json,txt, or geojson to support loading via GeoEvent 
- Should also support loading centers as points

## Create Landgrids

- Started with creation of grids over the entire world.  Created geojson using [createGeoJsonPolyGrids.py](https://github.com/david618/createTestData)
- Download 110m Geojson for country boundaries from [openlayers](https://github.com/openlayers/openlayers/blob/master/examples/data/geojson/countries-110m.geojson)
- Imported each of the GeoJson files to [QGIS](http://www.qgis.org/en/site/)
- Removed Antarctica from the country boundaries. I don't want to create ellipses in Antarctica.
- Converted the GeoJSON to Shapefiles for both datasets. The spatial queries are much faster with Shapefiles than GeoJSON. 
- Ran a query to select one degree grids that intersect the country boundaries. 
- Saved the intersection as a CSV file; just the Lower Left Lat and Long. 

## The Service

The [ellipse servlet](/src/main/java/org/jennings/websats/ellipses.java) loads in the CSV file created above. When the servlet is called it picks a random lower left corner and then create an ellipse in that grid. The ellipse will have a random semi-major, semi-minor, and rotation. They are hard coded for 0.1 to 1km.

The json has been configured to return two geometries. The center point and the ellipse using Esri Geometry types. 

The delimited text and geojson returns just the ellipses.

Input Parameters
- f : format type text, json, or geojson (default text)
- num : Number of ellipses to return (max is 5,000; default 1)
- npts: Number of points in the ellipse (default 50)





