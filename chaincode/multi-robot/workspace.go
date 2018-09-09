/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package main

import (

)

var (
	workspaceDef =
		`{
		   "start": {
		      "x": 1,
		      "y": 5
		   },
		   "goal": {
		      "x": 9,
		      "y": 1
		   },
		   "bounds": {
		      "xMin": 0,
		      "xMax": 10,
		      "yMin": 0,
		      "yMax": 7
		   },
		   "circles": [
		      {
		         "xCenter": 6,
		         "yCenter": 2,
		         "radius": 0.9
		      },
		      {
		         "xCenter": 4.5,
		         "yCenter": 5.5,
		         "radius": 0.6
		      }
		   ],
		   "rectangles": [
		      {
		         "xCenter": 2.5,
		         "yCenter": 2,
		         "width": 1.4,
		         "height": 1.4
		      },
		      {
		         "xCenter": 4,
		         "yCenter": 3.5,
		         "width": 1,
		         "height": 0.4
		      },
		      {
		         "xCenter": 7.5,
		         "yCenter": 4,
		         "width": 1.4,
		         "height": 1
		      }
		   ]
		}`
)
