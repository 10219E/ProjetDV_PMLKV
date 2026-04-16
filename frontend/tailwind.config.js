module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
	extend: {
	  screens: {
		// custom 'mid' breakpoint used to switch layout at 1273px as requested
		mid: '1273px'
	  }
	}
  },
  plugins: []
}
