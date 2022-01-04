
# OpenLayers free map version


Start with this: https://openlayers.org/en/latest/doc/tutorials/bundle.html

You're going to need `git` installed, and a newish version of `npm`.

Do:
```
npx create-ol-app map
```

- That will make a starter project in a new folder called `map`.
- Then copy all the files in this folder into that `map` folder (overwrite them).


Most of the brains of this thing are in the `main.js`.  `shared.js` is where it connects to.



## Running

If you do `npm start` that will start a test dev webserver on port 3000, and you can see the map in action!


![blah](map-overview.png)



## TODO

- taxi icon rotation / scaling / opacity fade-out when done
- ride hailed / started / ended icons
- popup info box
