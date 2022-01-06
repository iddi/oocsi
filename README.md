# Welcome to OOCSI!

![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.png?v=103) 

The OOCSI mission is to create a simple systems-interaction fabric for use by designers in academia and industry.

OOCSI is a prototyping middleware for designing distributed products and it is targeted mainly at Industrial Design and Computer Science education. OOCSI supports multiple client platforms that allow prototyping connected products and systemic designs with various heterogeneous components from embedded to mobile to the cloud. In the past years, OOCSI has been used by hundreds of students, educators, and researchers, and it is continuously developed as an open-source project with feedback from the community.

There are many cases that OOCSI can support connected prototyping and communication between devices and platforms. For example, connecting a sensor on an embedded platform to a data logger or visualisation on a mobile device. Or connecting the sensor to other devices in the same context to share data within a local system. Using OOCSI is very simple and we have example code that covers many application cases.

## Getting started

Our [wiki](https://github.com/iddi/oocsi/wiki) explains how to [get started](https://github.com/iddi/oocsi/wiki/Getting-started). Essentially, you need a server (or run your own) and then pick a client implementation for the platform that you are prototyping with (see below). For example, if you are designing a prototype based on an ESP32 and you would like to send data from this prototype to a Processing sketch to visualize the data, then you need to check out [OOCSI for ESP](https://github.com/iddi/oocsi-esp) and [OOCSI for Processing](https://github.com/iddi/oocsi-processing) to get started.

## Supported platforms

OOCSI supportes connected prototyping for the platforms or programming languages below. All repositories below have introductory information about how to connect to OOCSI.

* Processing ([GitHub repository](https://github.com/iddi/oocsi-processing))
* Java/Android ([GitHub repository](https://github.com/iddi/oocsi))
* Python ([GitHub repository](https://github.com/iddi/oocsi-python))
* Micropython (ESP) ([GitHub repository](https://github.com/iddi/oocsi-micropython))
* Javascript (HTML/Web) ([GitHub repository](https://github.com/iddi/oocsi-websocket))
* Javascript (Node.js) ([GitHub repository](https://github.com/iddi/oocsi-nodejs))
* Arduino/ESP (via Arduino IDE) ([GitHub repository](https://github.com/iddi/oocsi-esp))

Find more information on client support [here](https://github.com/iddi/oocsi/wiki/OOCSI-clients). If you don't see what you looking for, contact us or create an issue with a feature request. Also, implementing your own platform client for OOCSI is straightforward, check the existing clients or the [protocol](https://github.com/iddi/oocsi/wiki/OOCSI-Protocol) for more information.

## Cite as

Funk, Mathias. (2019, May). OOCSI. Zenodo. http://doi.org/10.5281/zenodo.1321220

![Zenodo badge](https://zenodo.org/badge/DOI/10.5281/zenodo.1321220.svg)
[![License: CC BY-SA 4.0](https://licensebuttons.net/l/by-sa/4.0/80x15.png)](https://creativecommons.org/licenses/by-sa/4.0/)

### BibTex

	@misc{funk_mathias_2019_1321220,
	  author       = {Funk, Mathias},
	  title        = {OOCSI},
	  month        = may,
	  year         = 2019,
	  doi          = {10.5281/zenodo.1321220},
	  url          = {https://doi.org/10.5281/zenodo.1321220}
	}

